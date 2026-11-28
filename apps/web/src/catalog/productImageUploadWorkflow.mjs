export function hasValidProductImageSelection(files) {
  return (
    files.length > 0 &&
    files.length <= 20 &&
    files.every(
      (file) =>
        file.size > 0 &&
        file.size <= 5 * 1024 * 1024 &&
        ["image/jpeg", "image/png", "image/webp"].includes(file.type),
    )
  );
}

export async function uploadAndAssignProductImages({ api, businessId, ready, unmatched }) {
  const pending = new Set();
  const failed = [...unmatched];
  let assignedCount = 0;

  try {
    const form = new FormData();
    ready.forEach(({ file }) => form.append("files", file));
    const response = await api(`/api/v1/businesses/${businessId}/product-images/bulk`, {
      method: "POST",
      body: form,
    });
    if (!response.ok) throw new Error("Image upload failed.");

    const uploaded = await response.json();
    uploaded.forEach((image) => pending.add(image.imageUrl));

    for (let index = 0; index < uploaded.length; index++) {
      const image = uploaded[index];
      const product = ready[index].product;
      const assigned = await api(`/api/v1/businesses/${businessId}/product-images/${product.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ imageUrl: image.imageUrl }),
      });
      if (assigned.ok) {
        assignedCount++;
        pending.delete(image.imageUrl);
      } else {
        failed.push(image.filename);
      }
    }
    return { assignedCount, failed };
  } finally {
    await Promise.all(
      [...pending].map(async (imageUrl) => {
        try {
          await api(
            `/api/v1/businesses/${businessId}/product-images/uploads?imageUrl=${encodeURIComponent(
              imageUrl,
            )}`,
            { method: "DELETE" },
          );
        } catch {
          // The backend cleanup queue handles durable object cleanup.
        }
      }),
    );
  }
}
