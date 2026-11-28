import { useState } from "react";
import { useApi } from "../auth/api";
import { matchProductImages, type ProductImageTarget } from "./productImageMatching.mjs";
import {
  hasValidProductImageSelection,
  uploadAndAssignProductImages,
} from "./productImageUploadWorkflow.mjs";

type Props = {
  businessId: string;
  products: ProductImageTarget[];
  language: "en" | "fr";
  onAssigned: () => Promise<void>;
};

const words = {
  en: {
    title: "Upload product photos",
    help: "Match photos by product code or CSV image filename.",
    upload: "Upload matched photos",
    uploading: "Uploading photos",
    matched: "photos ready to match",
    unmatched: "Not matched",
    ambiguous: "Ambiguous match",
    failed: "Some photos could not be uploaded or matched.",
    invalid: "Select up to 20 JPEG, PNG or WebP photos, each no larger than 5 MB.",
  },
  fr: {
    title: "Importer les photos des produits",
    help: "Associez les photos avec le code produit ou le nom du fichier CSV.",
    upload: "Importer les photos associées",
    uploading: "Import des photos",
    matched: "photos prêtes à associer",
    unmatched: "Non associée",
    ambiguous: "Association ambiguë",
    failed: "Certaines photos n'ont pas pu être importées ou associées.",
    invalid: "20 photos maximum, JPEG, PNG ou WebP, 5 Mo chacune.",
  },
} as const;

export default function ProductImageUploader({
  businessId,
  products,
  language,
  onAssigned,
}: Props) {
  const api = useApi();
  const t = words[language];
  const [files, setFiles] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const matches = matchProductImages(files, products);

  function selectFiles(selected: File[]) {
    if (!hasValidProductImageSelection(selected)) {
      setError(t.invalid);
      setFiles([]);
      return;
    }

    setError("");
    setMessage("");
    setFiles(selected);
  }

  async function upload() {
    const ready = matches.flatMap(({ file, product }) => (product ? [{ file, product }] : []));
    if (ready.length === 0) return;

    setBusy(true);
    setError("");
    setMessage("");
    try {
      const result = await uploadAndAssignProductImages({
        api,
        businessId,
        ready,
        unmatched: matches.filter((match) => !match.product).map((match) => match.file.name),
      });
      await onAssigned();
      setMessage(`${result.assignedCount} ${t.matched}`);
      if (result.failed.length) setError(`${t.unmatched}: ${result.failed.join(", ")}`);
      setFiles([]);
    } catch {
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="product-import">
      <div>
        <strong>{t.title}</strong>
        <p>{t.help}</p>
      </div>
      <input
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        disabled={busy}
        onChange={(event) => selectFiles(Array.from(event.target.files ?? []))}
      />
      {files.length > 0 && (
        <div className="product-import-result">
          <strong>
            {matches.filter((match) => match.product).length} {t.matched}
          </strong>
          {matches.map(({ file, product, ambiguous }) => (
            <p key={file.name}>
              {file.name} {"→"} {product?.name ?? (ambiguous ? t.ambiguous : t.unmatched)}
            </p>
          ))}
        </div>
      )}
      <button
        className="subtle-button"
        disabled={files.length === 0 || busy || !matches.some((match) => match.product)}
        onClick={() => void upload()}
      >
        {busy ? t.uploading : t.upload}
      </button>
      {message && <p>{message}</p>}
      {error && (
        <p className="product-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
