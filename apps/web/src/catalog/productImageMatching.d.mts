export type ProductImageTarget = {
  id: string;
  productCode: string;
  imageFilename: string | null;
  name: string;
};

export type ProductImageMatch = {
  file: File;
  product: ProductImageTarget | undefined;
  ambiguous: boolean;
};

export function matchProductImages(
  files: File[],
  products: ProductImageTarget[],
): ProductImageMatch[];
