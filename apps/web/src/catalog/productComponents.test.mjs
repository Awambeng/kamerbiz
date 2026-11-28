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

async function loadComponent(path, mocks = {}) {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const importsRemoved = source
    .replace(
      /import \{[^}]+\} from "react";/,
      "const { useCallback, useEffect, useState } = globalThis.__componentMocks.React;",
    )
    .replace(
      /import \{ useApi \} from "\.\.\/auth\/api";/,
      "const { useApi } = globalThis.__componentMocks;",
    )
    .replace(
      /import ProductImageUploader from "\.\/ProductImageUploader";/,
      "const ProductImageUploader = globalThis.__componentMocks.ProductImageUploader;",
    )
    .replace(/import "\.\/products\.css";/, "")
    .replace(
      /import \{[^}]+\} from "\.\/productImageMatching\.mjs";/,
      "const { matchProductImages } = globalThis.__componentMocks;",
    )
    .replace(
      /import \{[\s\S]*?\} from "\.\/productImageUploadWorkflow\.mjs";/,
      "const { hasValidProductImageSelection, uploadAndAssignProductImages } = globalThis.__componentMocks;",
    );

  globalThis.__componentMocks = {
    React,
    useApi: mocks["../auth/api"]?.useApi,
    ProductImageUploader: mocks["./ProductImageUploader"]?.default,
    ...mocks["./productImageMatching.mjs"],
    ...mocks["./productImageUploadWorkflow.mjs"],
  };
  const compiled = await transformWithOxc(importsRemoved, path, {
    jsx: { runtime: "automatic" },
  });
  const code = compiled.code.replaceAll('"react/jsx-runtime"', JSON.stringify(jsxRuntimeUrl));
  const encoded = Buffer.from(code).toString("base64");
  const componentModule = await import(`data:text/javascript;base64,${encoded}`);
  return componentModule.default;
}

test("product page renders catalogue and CSV import controls for business owners", async () => {
  const ProductPage = await loadComponent("./ProductPage.tsx", {
    "../auth/api": { useApi: () => async () => ({ ok: true, json: async () => [] }) },
    "./ProductImageUploader": {
      __esModule: true,
      default: () => React.createElement("div", null, "Image uploader"),
    },
    "./products.css": {},
  });

  const html = renderToStaticMarkup(
    React.createElement(ProductPage, {
      businessId: "business-1",
      role: "OWNER",
      language: "en",
    }),
  );

  assert.match(html, /Products/);
  assert.match(html, /Add product/);
  assert.match(html, /Import multiple products/);
  assert.match(html, /CSV file/);
  assert.match(html, /Image uploader/);
});

test("product page keeps staff in read only mode", async () => {
  const ProductPage = await loadComponent("./ProductPage.tsx", {
    "../auth/api": { useApi: () => async () => ({ ok: true, json: async () => [] }) },
    "./ProductImageUploader": {
      __esModule: true,
      default: () => React.createElement("div", null, "Image uploader"),
    },
    "./products.css": {},
  });

  const html = renderToStaticMarkup(
    React.createElement(ProductPage, {
      businessId: "business-1",
      role: "STAFF",
      language: "en",
    }),
  );

  assert.match(html, /view products/);
  assert.doesNotMatch(html, /Add product/);
  assert.doesNotMatch(html, /Import multiple products/);
});

test("product page renders its French catalogue and import labels", async () => {
  const ProductPage = await loadComponent("./ProductPage.tsx", {
    "../auth/api": { useApi: () => async () => ({ ok: true, json: async () => [] }) },
    "./ProductImageUploader": {
      __esModule: true,
      default: () => React.createElement("div", null, "Importateur d'images"),
    },
    "./products.css": {},
  });

  const html = renderToStaticMarkup(
    React.createElement(ProductPage, {
      businessId: "business-1",
      role: "MANAGER",
      language: "fr",
    }),
  );

  assert.match(html, /Produits/);
  assert.match(html, /Ajouter un produit/);
  assert.match(html, /Importer plusieurs produits/);
});

test("product image uploader renders disabled upload state before files are selected", async () => {
  const ProductImageUploader = await loadComponent("./ProductImageUploader.tsx", {
    "../auth/api": { useApi: () => async () => ({ ok: true }) },
    "./productImageMatching.mjs": {
      matchProductImages: () => [],
    },
    "./productImageUploadWorkflow.mjs": {
      hasValidProductImageSelection: () => true,
      uploadAndAssignProductImages: async () => ({ assignedCount: 0, failed: [] }),
    },
  });

  const html = renderToStaticMarkup(
    React.createElement(ProductImageUploader, {
      businessId: "business-1",
      products: [],
      language: "en",
      onAssigned: async () => {},
    }),
  );

  assert.match(html, /Upload product photos/);
  assert.match(html, /disabled/);
});

test("product image uploader renders French guidance", async () => {
  const ProductImageUploader = await loadComponent("./ProductImageUploader.tsx", {
    "../auth/api": { useApi: () => async () => ({ ok: true }) },
    "./productImageMatching.mjs": {
      matchProductImages: () => [],
    },
    "./productImageUploadWorkflow.mjs": {
      hasValidProductImageSelection: () => true,
      uploadAndAssignProductImages: async () => ({ assignedCount: 0, failed: [] }),
    },
  });

  const html = renderToStaticMarkup(
    React.createElement(ProductImageUploader, {
      businessId: "business-1",
      products: [],
      language: "fr",
      onAssigned: async () => {},
    }),
  );

  assert.match(html, /Importer les photos des produits/);
  assert.match(html, /Associez les photos/);
});
