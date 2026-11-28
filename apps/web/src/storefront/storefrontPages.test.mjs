import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import test from "node:test";
import { pathToFileURL } from "node:url";
import { transformWithOxc } from "vite";

const require = createRequire(import.meta.url);
const React = require("react");
const { renderToStaticMarkup } = require("react-dom/server");
const jsxRuntimeUrl = pathToFileURL(require.resolve("react/jsx-runtime")).href;
let moduleSequence = 0;

const orderStatusLabel = (language, value) => {
  const labels = {
    en: {
      NEW: "New",
      ACCEPTED: "Accepted",
      REJECTED: "Rejected",
      COMPLETED: "Completed",
      NOT_ARRANGED: "Not arranged",
      PREPARING: "Preparing",
      READY_FOR_PICKUP: "Ready for pickup",
      DISPATCHED: "Dispatched",
      DELIVERED: "Delivered",
    },
    fr: {
      NEW: "Nouvelle",
      ACCEPTED: "Acceptée",
      REJECTED: "Refusée",
      COMPLETED: "Terminée",
      NOT_ARRANGED: "Non planifiée",
      PREPARING: "En préparation",
      READY_FOR_PICKUP: "Prête au retrait",
      DISPATCHED: "Expédiée",
      DELIVERED: "Livrée",
    },
  };
  return labels[language][value] ?? value.toLowerCase();
};

const nextFulfilmentStatuses = (method, current) => {
  if (current === "PREPARING") {
    return method === "PICKUP" ? ["READY_FOR_PICKUP"] : ["DISPATCHED"];
  }
  if (current === "READY_FOR_PICKUP" && method === "PICKUP") return ["COMPLETED"];
  if (current === "DISPATCHED" && method === "DELIVERY") return ["DELIVERED"];
  return [];
};

function mockReact(stateValues = []) {
  let stateIndex = 0;
  return {
    createElement: React.createElement,
    useState(initialValue) {
      const index = stateIndex++;
      return [index < stateValues.length ? stateValues[index] : initialValue, () => {}];
    },
    useEffect() {},
    useCallback(callback) {
      return callback;
    },
  };
}

async function loadPage(path, stateValues = []) {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const importsRemoved = source
    .replace(
      /import \{[^}]+\} from "react";/,
      "const { useCallback, useEffect, useState } = globalThis.__pageMocks.React;",
    )
    .replace(
      /import \{ useApi \} from "\.\.\/auth\/api";/,
      "const { useApi } = globalThis.__pageMocks;",
    )
    .replace(
      /import \{ nextFulfilmentStatuses, orderStatusLabel \} from "\.\/orderStatus";/,
      "const { nextFulfilmentStatuses, orderStatusLabel } = globalThis.__pageMocks;",
    )
    .replace(
      /import \{ orderStatusLabel \} from "\.\.\/orders\/orderStatus";/,
      "const { orderStatusLabel } = globalThis.__pageMocks;",
    )
    .replace(/import "\.\/orders\.css";/, "")
    .replace(/import "\.\/storefront\.css";/, "");

  globalThis.__pageMocks = {
    React: mockReact(stateValues),
    useApi: () => async () => ({ ok: true, json: async () => [] }),
    orderStatusLabel,
    nextFulfilmentStatuses,
  };

  const compiled = await transformWithOxc(importsRemoved, path, {
    jsx: { runtime: "automatic" },
  });
  const code = compiled.code.replaceAll('"react/jsx-runtime"', JSON.stringify(jsxRuntimeUrl));
  const encoded = Buffer.from(`${code}\n// module ${moduleSequence++}`).toString("base64");
  const componentModule = await import(`data:text/javascript;base64,${encoded}`);
  return componentModule.default;
}

const store = {
  name: "Grace Fashion",
  slug: "grace-fashion",
  description: "Everyday fashion, made easy.",
  logoUrl: null,
  city: "Douala",
  region: "Littoral",
  phone: "677000000",
  whatsapp: "677000000",
  products: [
    {
      id: "product-1",
      name: "Blue Jeans",
      description: "Classic denim",
      category: "Fashion",
      price: 12000,
      stockQuantity: 5,
      imageUrl: null,
    },
  ],
};

test("vendor orders page shows loading instead of a premature empty state", async () => {
  const OrdersPage = await loadPage("../orders/OrdersPage.tsx");
  const html = renderToStaticMarkup(
    React.createElement(OrdersPage, { businessId: "business-1", language: "en" }),
  );

  assert.match(html, /Loading orders/);
  assert.doesNotMatch(html, /No orders yet/);
});

test("vendor orders page renders order details and French status labels", async () => {
  const order = {
    id: "order-1",
    reference: "KB-123456789ABC",
    customerName: "Customer",
    phone: "677000000",
    fulfilmentMethod: "PICKUP",
    deliveryAddress: null,
    note: null,
    status: "ACCEPTED",
    fulfilmentStatus: "PREPARING",
    total: 24000,
    createdAt: "2026-11-28T10:00:00Z",
    items: [{ name: "Blue Jeans", unitPrice: 12000, quantity: 2, total: 24000 }],
  };
  const OrdersPage = await loadPage("../orders/OrdersPage.tsx", [[order], null, false, ""]);
  const html = renderToStaticMarkup(
    React.createElement(OrdersPage, { businessId: "business-1", language: "fr" }),
  );

  assert.match(html, /KB-123456789ABC/);
  assert.match(html, /Acceptée/);
  assert.match(html, /En préparation/);
  assert.match(html, /Prête au retrait/);
});

test("public storefront renders the business catalogue and contact details", async () => {
  globalThis.window = { location: { pathname: "/store/grace-fashion" } };
  const StorefrontPage = await loadPage("./StorefrontPage.tsx", ["en", store, false, "", {}]);
  const html = renderToStaticMarkup(React.createElement(StorefrontPage));

  assert.match(html, /Grace Fashion/);
  assert.match(html, /Blue Jeans/);
  assert.match(html, /FCFA/);
  assert.match(html, /tel:677000000/);
  assert.match(html, /WhatsApp/);
});

test("public storefront provides guest checkout without requiring an account", async () => {
  globalThis.window = { location: { pathname: "/store/grace-fashion" } };
  const StorefrontPage = await loadPage("./StorefrontPage.tsx", [
    "en",
    store,
    false,
    "",
    { "product-1": 1 },
    true,
  ]);
  const html = renderToStaticMarkup(React.createElement(StorefrontPage));

  assert.match(html, /Checkout/);
  assert.match(html, /Your name/);
  assert.match(html, /Phone number/);
  assert.match(html, /Delivery address/);
  assert.match(html, /Place order/);
  assert.doesNotMatch(html, /Sign in|Create account/);
});

test("customer tracking page renders the translated order progress", async () => {
  globalThis.window = { location: { pathname: "/track/private-token" } };
  const TrackingPage = await loadPage("./OrderTrackingPage.tsx", [
    {
      reference: "KB-123456789ABC",
      status: "ACCEPTED",
      fulfilmentStatus: "READY_FOR_PICKUP",
    },
    "fr",
    false,
  ]);
  const html = renderToStaticMarkup(React.createElement(TrackingPage));

  assert.match(html, /KB-123456789ABC/);
  assert.match(html, /Acceptée/);
  assert.match(html, /Prête au retrait/);
});
