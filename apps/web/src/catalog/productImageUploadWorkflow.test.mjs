import assert from "node:assert/strict";
import test from "node:test";
import {
  hasValidProductImageSelection,
  uploadAndAssignProductImages,
} from "./productImageUploadWorkflow.mjs";

const file = (name = "product.png", type = "image/png", size = 100) =>
  new File([new Uint8Array(size)], name, { type });

test("accepts supported images within the per file and batch limits", () => {
  assert.equal(hasValidProductImageSelection([file()]), true);
  assert.equal(hasValidProductImageSelection([file("photo.webp", "image/webp")]), true);
});

test("rejects empty, unsupported, oversized and over twenty file selections", () => {
  assert.equal(hasValidProductImageSelection([]), false);
  assert.equal(hasValidProductImageSelection([file("bad.svg", "image/svg+xml")]), false);
  assert.equal(
    hasValidProductImageSelection([file("large.png", "image/png", 5 * 1024 * 1024 + 1)]),
    false,
  );
  assert.equal(
    hasValidProductImageSelection(Array.from({ length: 21 }, (_, index) => file(`${index}.png`))),
    false,
  );
});

test("uploads matched images and assigns each image to its product", async () => {
  const calls = [];
  const api = async (url, options) => {
    calls.push({ url, options });
    if (options.method === "POST") {
      return {
        ok: true,
        json: async () => [
          { filename: "JEANS-1.png", imageUrl: "https://cdn/jeans.png" },
          { filename: "SHIRT-2.png", imageUrl: "https://cdn/shirt.png" },
        ],
      };
    }
    return { ok: true };
  };

  const result = await uploadAndAssignProductImages({
    api,
    businessId: "business-1",
    ready: [
      { file: file("JEANS-1.png"), product: { id: "product-1" } },
      { file: file("SHIRT-2.png"), product: { id: "product-2" } },
    ],
    unmatched: ["unknown.png"],
  });

  assert.equal(result.assignedCount, 2);
  assert.deepEqual(result.failed, ["unknown.png"]);
  assert.equal(calls.filter(({ options }) => options.method === "PATCH").length, 2);
  assert.equal(
    calls.some(({ options }) => options.method === "DELETE"),
    false,
  );
});

test("cleans up images that could not be assigned", async () => {
  const calls = [];
  const api = async (url, options) => {
    calls.push({ url, options });
    if (options.method === "POST") {
      return {
        ok: true,
        json: async () => [{ filename: "JEANS-1.png", imageUrl: "https://cdn/jeans.png" }],
      };
    }
    if (options.method === "PATCH") return { ok: false };
    return { ok: true };
  };

  const result = await uploadAndAssignProductImages({
    api,
    businessId: "business-1",
    ready: [{ file: file(), product: { id: "product-1" } }],
    unmatched: [],
  });

  assert.equal(result.assignedCount, 0);
  assert.deepEqual(result.failed, ["JEANS-1.png"]);
  assert.equal(
    calls.some(({ options }) => options.method === "DELETE"),
    true,
  );
});

test("cleans up uploaded objects when an assignment request throws", async () => {
  const calls = [];
  const api = async (url, options) => {
    calls.push({ url, options });
    if (options.method === "POST") {
      return {
        ok: true,
        json: async () => [{ filename: "JEANS-1.png", imageUrl: "https://cdn/jeans.png" }],
      };
    }
    if (options.method === "PATCH") throw new Error("Network unavailable");
    return { ok: true };
  };

  await assert.rejects(
    uploadAndAssignProductImages({
      api,
      businessId: "business-1",
      ready: [{ file: file(), product: { id: "product-1" } }],
      unmatched: [],
    }),
    /Network unavailable/,
  );
  assert.equal(
    calls.some(({ options }) => options.method === "DELETE"),
    true,
  );
});
