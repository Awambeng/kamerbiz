export function matchProductImages(files, products) {
  const claimed = new Set();

  return files.map((file) => {
    const filename = file.name.toLowerCase();
    const stem = filename.replace(/\.[^.]+$/, "");
    const candidates = products.filter((product) => {
      const code = product.productCode.toLowerCase();
      const mapped = product.imageFilename?.toLowerCase();
      return code === stem || mapped === filename || mapped === stem;
    });
    const candidate = candidates.length === 1 ? candidates[0] : undefined;
    const duplicate = candidate ? claimed.has(candidate.id) : false;
    if (candidate && !duplicate) claimed.add(candidate.id);

    return {
      file,
      product: duplicate ? undefined : candidate,
      ambiguous: candidates.length > 1 || duplicate,
    };
  });
}
