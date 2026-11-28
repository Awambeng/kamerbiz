type ProductImageFile = Pick<File, "name" | "type" | "size">;
type ProductImageTarget = { id: string };
type Api = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export function hasValidProductImageSelection(files: ProductImageFile[]): boolean;

export function uploadAndAssignProductImages(args: {
  api: Api;
  businessId: string;
  ready: { file: File; product: ProductImageTarget }[];
  unmatched: string[];
}): Promise<{ assignedCount: number; failed: string[] }>;
