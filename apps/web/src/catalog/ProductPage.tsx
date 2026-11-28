import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useApi } from "../auth/api";
import ProductImageUploader from "./ProductImageUploader";
import "./products.css";

type Product = {
  id: string;
  productCode: string;
  name: string;
  description: string | null;
  category: string | null;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  imageFilename: string | null;
  archived: boolean;
};

type UploadedImage = {
  filename: string;
  imageUrl: string;
};

type ImportPreview = {
  totalRows: number;
  validRows: number;
  rejectedRows: number;
  errors: string[];
};

type ImportResult = {
  createdCount: number;
  skippedCount: number;
  replayed: boolean;
  errors: string[];
};

type Props = {
  businessId: string;
  role: "OWNER" | "MANAGER" | "STAFF";
  language: "en" | "fr";
};

type Draft = {
  productCode: string;
  name: string;
  description: string;
  category: string;
  price: string;
  stockQuantity: string;
  imageUrl: string;
};

const empty: Draft = {
  productCode: "",
  name: "",
  description: "",
  category: "",
  price: "",
  stockQuantity: "0",
  imageUrl: "",
};

const words = {
  en: {
    title: "Products",
    intro: "Keep your products and stock organised.",
    add: "Add product",
    name: "Product name",
    code: "Product code (optional)",
    codeHelp: "KamerBiz creates a code automatically if you leave this blank.",
    category: "Category",
    price: "Price (FCFA)",
    stock: "Quantity in stock",
    description: "Description",
    image: "Product photo (optional)",
    imageTooLarge: "Each photo must be 5 MB or smaller. Use JPEG, PNG or WebP.",
    photoUploaded: "Photo uploaded",
    save: "Save product",
    cancel: "Cancel",
    search: "Search products",
    active: "Active products",
    archived: "Archived",
    empty: "No products yet. Add your first product to get started.",
    inStock: "In stock",
    out: "Out of stock",
    archive: "Archive",
    restore: "Restore",
    failed: "Something went wrong. Please try again.",
    permission: "You can view products, but only owners and managers can make changes.",
    required: "Enter a product name and a valid price.",
    importTitle: "Import multiple products",
    importHelp: "Upload a CSV file prepared with Excel or Google Sheets.",
    importAction: "Check file",
    confirmImport: "Import valid products",
    previewReady: "Review your file before importing",
    validRows: "valid rows",
    rejectedRows: "rows need correction",
    downloadErrors: "Download error report",
    created: "created",
    skipped: "skipped",
    edit: "Edit",
    template: "CSV template",
    units: "units",
  },
  fr: {
    title: "Produits",
    intro: "Organisez vos produits et votre stock.",
    add: "Ajouter un produit",
    name: "Nom du produit",
    code: "Code produit (facultatif)",
    codeHelp: "KamerBiz crée automatiquement un code si vous laissez ce champ vide.",
    category: "Catégorie",
    price: "Prix (FCFA)",
    stock: "Quantité en stock",
    description: "Description",
    image: "Photo du produit (facultatif)",
    imageTooLarge: "Chaque photo doit faire 5 Mo maximum. Utilisez JPEG, PNG ou WebP.",
    photoUploaded: "Photo importée",
    save: "Enregistrer",
    cancel: "Annuler",
    search: "Rechercher des produits",
    active: "Produits actifs",
    archived: "Archivés",
    empty: "Aucun produit. Ajoutez votre premier produit pour commencer.",
    inStock: "En stock",
    out: "Rupture de stock",
    archive: "Archiver",
    restore: "Restaurer",
    failed: "Une erreur est survenue. Veuillez réessayer.",
    permission: "Consultation autorisée. Modification réservée aux propriétaires et gestionnaires.",
    required: "Saisissez un nom et un prix valide.",
    importTitle: "Importer plusieurs produits",
    importHelp: "Importez un fichier CSV préparé avec Excel ou Google Sheets.",
    importAction: "Vérifier le fichier",
    confirmImport: "Importer les produits valides",
    previewReady: "Vérifiez votre fichier avant l'importation",
    validRows: "lignes valides",
    rejectedRows: "lignes à corriger",
    downloadErrors: "Télécharger le rapport d'erreurs",
    created: "créés",
    skipped: "ignorés",
    edit: "Modifier",
    template: "Modèle CSV",
    units: "unités",
  },
} as const;

export default function ProductPage({ businessId, role, language }: Props) {
  const api = useApi();
  const t = words[language];
  const canEdit = role !== "STAFF";
  const [items, setItems] = useState<Product[]>([]);
  const [archived, setArchived] = useState(false);
  const [search, setSearch] = useState("");
  const [draft, setDraft] = useState<Draft>(empty);
  const [editing, setEditing] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importKey, setImportKey] = useState("");
  const [importPreview, setImportPreview] = useState<ImportPreview | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [pendingImageUrl, setPendingImageUrl] = useState<string | null>(null);
  const [originalImageUrl, setOriginalImageUrl] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api(`/api/v1/businesses/${businessId}/products?includeArchived=true`);
      if (!response.ok) throw new Error();
      setItems((await response.json()) as Product[]);
    } catch {
      setError(t.failed);
    } finally {
      setLoading(false);
    }
  }, [api, businessId, t.failed]);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = items.filter(
    (product) =>
      product.archived === archived &&
      [product.name, product.productCode, product.category ?? ""].some((value) =>
        value.toLowerCase().includes(search.toLowerCase()),
      ),
  );

  function edit(product: Product) {
    setEditing(product.id);
    setOriginalImageUrl(product.imageUrl);
    setDraft({
      productCode: product.productCode,
      name: product.name,
      description: product.description ?? "",
      category: product.category ?? "",
      price: String(product.price),
      stockQuantity: String(product.stockQuantity),
      imageUrl: product.imageUrl ?? "",
    });
    setFormOpen(true);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (
      !draft.name.trim() ||
      !Number.isFinite(Number(draft.price)) ||
      Number(draft.price) <= 0 ||
      Number(draft.stockQuantity) < 0
    ) {
      setError(t.required);
      return;
    }

    setBusy(true);
    setError("");
    try {
      const response = await api(
        `/api/v1/businesses/${businessId}/products${editing ? `/${editing}` : ""}`,
        {
          method: editing ? "PATCH" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            ...draft,
            price: Number(draft.price),
            stockQuantity: Number(draft.stockQuantity),
          }),
        },
      );
      if (!response.ok) throw new Error();
      const product = (await response.json()) as Product;
      setItems((current) =>
        editing
          ? current.map((item) => (item.id === product.id ? product : item))
          : [product, ...current],
      );
      setDraft(empty);
      setEditing(null);
      setFormOpen(false);
      setPendingImageUrl(null);
      setOriginalImageUrl(null);
    } catch {
      if (pendingImageUrl) {
        await deleteUploadedImage(pendingImageUrl);
        setPendingImageUrl(null);
        setDraft((current) => ({ ...current, imageUrl: originalImageUrl ?? "" }));
      }
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  async function toggleArchive(product: Product) {
    try {
      const action = product.archived ? "restore" : "archive";
      const response = await api(
        `/api/v1/businesses/${businessId}/products/${product.id}/${action}`,
        { method: "POST" },
      );
      if (!response.ok) throw new Error();
      const updated = (await response.json()) as Product;
      setItems((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    } catch {
      setError(t.failed);
    }
  }

  async function deleteUploadedImage(imageUrl: string) {
    try {
      await api(
        `/api/v1/businesses/${businessId}/product-images/uploads?imageUrl=${encodeURIComponent(imageUrl)}`,
        { method: "DELETE" },
      );
    } catch {
      // A failed cleanup is safe to retry and must not hide the original operation result.
    }
  }

  async function uploadSingleImage(file: File) {
    if (
      file.size > 5 * 1024 * 1024 ||
      !["image/jpeg", "image/png", "image/webp"].includes(file.type)
    ) {
      setError(t.imageTooLarge);
      return;
    }
    setBusy(true);
    setError("");
    try {
      const data = new FormData();
      data.append("file", file);
      const uploadRequest: RequestInit = {
        method: "POST",
        body: data,
      };
      const response = await api(`/api/v1/businesses/${businessId}/product-images`, uploadRequest);
      if (!response.ok) throw new Error();
      const uploaded = (await response.json()) as UploadedImage;
      if (pendingImageUrl) await deleteUploadedImage(pendingImageUrl);
      setPendingImageUrl(uploaded.imageUrl);
      setDraft((current) => ({ ...current, imageUrl: uploaded.imageUrl }));
    } catch {
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  async function previewCsv() {
    if (!importFile) return;
    setBusy(true);
    setError("");
    setImportResult(null);
    try {
      const previewRequest: RequestInit = {
        method: "POST",
        headers: { "Content-Type": "text/csv" },
        body: await importFile.text(),
      };
      const response = await api(
        `/api/v1/businesses/${businessId}/products/import/preview`,
        previewRequest,
      );
      if (!response.ok) throw new Error();
      setImportPreview((await response.json()) as ImportPreview);
    } catch {
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  async function importCsv() {
    if (!importFile || !importPreview || importPreview.validRows === 0) return;
    setBusy(true);
    setError("");
    try {
      const importRequest: RequestInit = {
        method: "POST",
        headers: {
          "Content-Type": "text/csv",
          "X-Import-Key": importKey,
        },
        body: await importFile.text(),
      };
      const response = await api(`/api/v1/businesses/${businessId}/products/import`, importRequest);
      if (!response.ok) throw new Error();
      setImportResult((await response.json()) as ImportResult);
      setImportFile(null);
      setImportPreview(null);
      setImportKey("");
      await load();
    } catch {
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  function downloadImportErrors() {
    if (!importPreview?.errors.length) return;
    const csv = ["error", ...importPreview.errors]
      .map((value) => `"${value.replaceAll('"', '""')}"`)
      .join("\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "kamerbiz-import-errors.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  function downloadTemplate() {
    const data = [
      "product_code,name,description,category,price,stock_quantity,image_filename",
      ",Blue Jeans,Classic blue jeans,Fashion,12000,15,jeans-blue.jpg",
    ].join("\n");
    const url = URL.createObjectURL(new Blob([data], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "kamerbiz-products-template.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <section className="products-page">
      <div className="products-heading">
        <div>
          <p className="eyebrow">KAMERBIZ CATALOG</p>
          <h2>{t.title}</h2>
          <p>{t.intro}</p>
        </div>
        {canEdit && (
          <button
            className="primary-button"
            disabled={busy || formOpen}
            onClick={() => {
              setDraft(empty);
              setEditing(null);
              setPendingImageUrl(null);
              setOriginalImageUrl(null);
              setFormOpen(true);
            }}
          >
            ＋ {t.add}
          </button>
        )}
      </div>

      <div className="products-toolbar">
        <input
          placeholder={t.search}
          aria-label={t.search}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <div className="product-tabs">
          <button className={!archived ? "active" : ""} onClick={() => setArchived(false)}>
            {t.active}
          </button>
          <button className={archived ? "active" : ""} onClick={() => setArchived(true)}>
            {t.archived}
          </button>
        </div>
        {canEdit && (
          <button className="subtle-button" onClick={downloadTemplate}>
            ↓ {t.template}
          </button>
        )}
      </div>

      {!canEdit && <p className="product-hint">{t.permission}</p>}

      {canEdit && (
        <div className="product-import">
          <div>
            <strong>{t.importTitle}</strong>
            <p>{t.importHelp}</p>
          </div>
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(event) => {
              setImportFile(event.target.files?.[0] ?? null);
              setImportKey(crypto.randomUUID());
              setImportResult(null);
              setImportPreview(null);
            }}
          />
          <button
            className="subtle-button"
            disabled={!importFile || busy}
            onClick={() => void previewCsv()}
          >
            {busy ? "..." : t.importAction}
          </button>
        </div>
      )}

      {canEdit && (
        <ProductImageUploader
          businessId={businessId}
          products={items}
          language={language}
          onAssigned={load}
        />
      )}

      {importPreview && (
        <div className="product-import-result">
          <strong>{t.previewReady}</strong>
          <p>
            {importPreview.validRows} {t.validRows}, {importPreview.rejectedRows} {t.rejectedRows}
          </p>
          {importPreview.errors.map((item, index) => (
            <p key={index}>{item}</p>
          ))}
          {importPreview.errors.length > 0 && (
            <button className="subtle-button" onClick={downloadImportErrors}>
              {t.downloadErrors}
            </button>
          )}
          <button
            className="primary-button"
            disabled={busy || importPreview.validRows === 0}
            onClick={() => void importCsv()}
          >
            {t.confirmImport}
          </button>
        </div>
      )}

      {importResult && (
        <div className="product-import-result">
          <strong>
            {importResult.createdCount} {t.created}, {importResult.skippedCount} {t.skipped}
          </strong>
          {importResult.errors.map((item, index) => (
            <p key={index}>{item}</p>
          ))}
        </div>
      )}

      {error && (
        <p className="product-error" role="alert">
          {error}
        </p>
      )}

      {formOpen && canEdit && (
        <form className="product-form" onSubmit={save}>
          <div className="product-form-heading">
            <h3>{editing ? t.edit : t.add}</h3>
            <button
              type="button"
              className="subtle-button"
              disabled={busy}
              onClick={() => {
                if (pendingImageUrl) void deleteUploadedImage(pendingImageUrl);
                setPendingImageUrl(null);
                setOriginalImageUrl(null);
                setDraft(empty);
                setEditing(null);
                setFormOpen(false);
              }}
            >
              {t.cancel}
            </button>
          </div>
          <label>
            {t.name}
            <input
              required
              maxLength={255}
              value={draft.name}
              onChange={(event) => setDraft({ ...draft, name: event.target.value })}
            />
          </label>
          <label>
            {t.code}
            <input
              maxLength={64}
              disabled={Boolean(editing)}
              value={draft.productCode}
              onChange={(event) => setDraft({ ...draft, productCode: event.target.value })}
            />
            {!editing && <small>{t.codeHelp}</small>}
          </label>
          <label>
            {t.category}
            <input
              maxLength={120}
              value={draft.category}
              onChange={(event) => setDraft({ ...draft, category: event.target.value })}
            />
          </label>
          <label>
            {t.price}
            <input
              required
              type="number"
              min="0.01"
              step="0.01"
              value={draft.price}
              onChange={(event) => setDraft({ ...draft, price: event.target.value })}
            />
          </label>
          <label>
            {t.stock}
            <input
              required
              type="number"
              min="0"
              step="1"
              value={draft.stockQuantity}
              onChange={(event) => setDraft({ ...draft, stockQuantity: event.target.value })}
            />
          </label>
          <label className="product-wide">
            {t.description}
            <textarea
              rows={3}
              value={draft.description}
              onChange={(event) => setDraft({ ...draft, description: event.target.value })}
            />
          </label>
          <label className="product-wide">
            {t.image}
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) void uploadSingleImage(file);
              }}
            />
            {draft.imageUrl && <small>{t.photoUploaded}</small>}
          </label>
          <div className="product-form-actions">
            <button className="primary-button" disabled={busy}>
              {busy ? "..." : t.save}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="product-empty">...</p>
      ) : visible.length === 0 ? (
        <p className="product-empty">{t.empty}</p>
      ) : (
        <div className="product-grid">
          {visible.map((product) => (
            <article className="product-card" key={product.id}>
              <div className="product-image">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt="" />
                ) : (
                  product.name.slice(0, 1).toUpperCase()
                )}
              </div>
              <div className="product-card-body">
                <div className="product-card-top">
                  <span className="product-code">{product.productCode}</span>
                  <span className={product.stockQuantity ? "stock-pill" : "stock-pill empty"}>
                    {product.stockQuantity ? t.inStock : t.out}
                  </span>
                </div>
                <h3>{product.name}</h3>
                <p className="product-category">{product.category ?? " "}</p>
                <div className="product-card-bottom">
                  <strong>
                    {product.price.toLocaleString(language === "fr" ? "fr-FR" : "en-US")} FCFA
                  </strong>
                  <span>
                    {product.stockQuantity} {t.units}
                  </span>
                </div>
                {canEdit && (
                  <div className="product-card-actions">
                    <button onClick={() => edit(product)}>{t.edit}</button>
                    <button onClick={() => void toggleArchive(product)}>
                      {product.archived ? t.restore : t.archive}
                    </button>
                  </div>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      <p className="product-count">
        {visible.length} {language === "fr" ? "produits" : "products"}
      </p>
    </section>
  );
}
