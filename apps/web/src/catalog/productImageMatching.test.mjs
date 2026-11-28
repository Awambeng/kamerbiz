import assert from "node:assert/strict";
import test from "node:test";
import { matchProductImages } from "./productImageMatching.mjs";

const product = {
  id: "product-1",
  productCode: "KB-001",
  imageFilename: "blue-jeans.jpg",
  name: "Blue Jeans",
};

test("matches an image by generated product code", () => {
  const [match] = matchProductImages([{ name: "kb-001.PNG" }], [product]);

  assert.equal(match.product?.id, product.id);
  assert.equal(match.ambiguous, false);
});

test("matches an image by the optional CSV image filename", () => {
  const [match] = matchProductImages([{ name: "blue-jeans.jpg" }], [product]);

  assert.equal(match.product?.id, product.id);
});

test("does not attach an image when multiple products match", () => {
  const second = { ...product, id: "product-2" };
  const [match] = matchProductImages([{ name: "blue-jeans.jpg" }], [product, second]);

  assert.equal(match.product, undefined);
  assert.equal(match.ambiguous, true);
});

test("does not assign multiple images to the same product", () => {
  const matches = matchProductImages([{ name: "KB-001.jpg" }, { name: "kb-001.png" }], [product]);

  assert.equal(matches[0].product?.id, product.id);
  assert.equal(matches[1].product, undefined);
  assert.equal(matches[1].ambiguous, true);
});

test("reports files with no matching product", () => {
  const [match] = matchProductImages([{ name: "unknown.jpg" }], [product]);

  assert.equal(match.product, undefined);
  assert.equal(match.ambiguous, false);
});
