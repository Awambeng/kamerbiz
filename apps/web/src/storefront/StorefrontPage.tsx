import { useEffect, useState, type FormEvent } from "react";
import "./storefront.css";

type Product = {
  id: string;
  name: string;
  description: string | null;
  category: string | null;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
};

type Store = {
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  city: string | null;
  region: string | null;
  phone: string | null;
  whatsapp: string | null;
  products: Product[];
};

const words = {
  en: {
    loading: "Opening store...",
    search: "Search products",
    add: "Add to basket",
    basket: "Basket",
    checkout: "Checkout",
    name: "Your name",
    phone: "Phone number",
    method: "Delivery or pickup",
    delivery: "Delivery",
    pickup: "Pickup",
    address: "Delivery address",
    place: "Place order",
    back: "Back to store",
    empty: "No products found",
    success: "Your order has been sent",
    failed: "Something went wrong. Please try again.",
    tagline: "LOCAL BUSINESS, EASY SHOPPING",
    discover: "DISCOVER THE STORE",
    products: "products",
    soldOut: "Sold out",
    track: "Track your order",
    items: "items",
    footer: "KamerBiz · Stronger businesses. A stronger Cameroon.",
    call: "Call store",
    whatsapp: "WhatsApp",
  },
  fr: {
    loading: "Ouverture de la boutique...",
    search: "Rechercher",
    add: "Ajouter au panier",
    basket: "Panier",
    checkout: "Commander",
    name: "Votre nom",
    phone: "Téléphone",
    method: "Livraison ou retrait",
    delivery: "Livraison",
    pickup: "Retrait",
    address: "Adresse de livraison",
    place: "Valider la commande",
    back: "Retour à la boutique",
    empty: "Aucun produit trouvé",
    success: "Votre commande a été envoyée",
    failed: "Une erreur est survenue. Veuillez réessayer.",
    tagline: "UNE ENTREPRISE LOCALE, FACILE À ACHETER",
    discover: "DÉCOUVRIR LA BOUTIQUE",
    products: "produits",
    soldOut: "Épuisé",
    track: "Suivre la commande",
    items: "articles",
    footer: "KamerBiz · Des entreprises plus fortes. Un Cameroun plus fort.",
    call: "Appeler la boutique",
    whatsapp: "WhatsApp",
  },
} as const;

const money = (price: number) => new Intl.NumberFormat("fr-CM").format(price) + " FCFA";

export default function StorefrontPage() {
  const slug = window.location.pathname.split("/").filter(Boolean)[1] ?? "";
  const [lang, setLang] = useState<"en" | "fr">("en");
  const t = words[lang];
  const [store, setStore] = useState<Store | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [cart, setCart] = useState<Record<string, number>>({});
  const [checkout, setCheckout] = useState(false);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [method, setMethod] = useState<"DELIVERY" | "PICKUP">("DELIVERY");
  const [address, setAddress] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [reference, setReference] = useState("");
  const [trackingUrl, setTrackingUrl] = useState("");

  useEffect(() => {
    fetch(`/api/v1/public/stores/${encodeURIComponent(slug)}`)
      .then(async (response) => {
        if (!response.ok) throw new Error();
        return response.json() as Promise<Store>;
      })
      .then(setStore)
      .catch(() => setError("failed"))
      .finally(() => setLoading(false));
  }, [slug]);

  const products = (store?.products ?? []).filter((product) =>
    [product.name, product.description ?? "", product.category ?? ""].some((value) =>
      value.toLowerCase().includes(query.toLowerCase()),
    ),
  );
  const lines = (store?.products ?? []).filter((product) => cart[product.id]);
  const total = lines.reduce((sum, product) => sum + product.price * cart[product.id], 0);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!store) return;
    setBusy(true);
    setError("");
    try {
      const response = await fetch(
        `/api/v1/public/stores/${encodeURIComponent(store.slug)}/orders`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            customerName: name,
            phone,
            fulfilmentMethod: method,
            deliveryAddress: method === "DELIVERY" ? address : null,
            items: lines.map((product) => ({
              productId: product.id,
              quantity: cart[product.id],
            })),
          }),
        },
      );
      if (!response.ok) throw new Error();
      const result = (await response.json()) as {
        reference: string;
        trackingUrl: string;
      };
      setReference(result.reference);
      setTrackingUrl(result.trackingUrl);
    } catch {
      setError(t.failed);
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <main className="public-store">
        <p>{t.loading}</p>
      </main>
    );
  }

  if (!store) {
    return (
      <main className="public-store">
        <h1>KamerBiz</h1>
        <p>{t.failed}</p>
      </main>
    );
  }

  return (
    <main className="public-store">
      <header className="public-store-header">
        <a href="/" className="public-store-brand">
          <span>K</span>
          KamerBiz
        </a>
        <div>
          <button onClick={() => setLang("en")} className={lang === "en" ? "chosen" : ""}>
            EN
          </button>
          <button onClick={() => setLang("fr")} className={lang === "fr" ? "chosen" : ""}>
            FR
          </button>
        </div>
      </header>

      {reference ? (
        <section className="public-store-success">
          <span>✓</span>
          <p className="public-store-kicker">{store.name}</p>
          <h1>{t.success}</h1>
          <strong>{reference}</strong>
          <a className="public-track-link" href={trackingUrl}>
            {t.track} →
          </a>
        </section>
      ) : checkout ? (
        <section className="public-checkout">
          <button className="public-back" onClick={() => setCheckout(false)}>
            ← {t.back}
          </button>
          <div className="public-checkout-grid">
            <form onSubmit={submit}>
              <p className="public-store-kicker">{store.name}</p>
              <h1>{t.checkout}</h1>
              <label>
                {t.name}
                <input
                  required
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  maxLength={160}
                />
              </label>
              <label>
                {t.phone}
                <input
                  required
                  type="tel"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  maxLength={32}
                />
              </label>
              <fieldset>
                <legend>{t.method}</legend>
                <label>
                  <input
                    type="radio"
                    checked={method === "DELIVERY"}
                    onChange={() => setMethod("DELIVERY")}
                  />
                  {t.delivery}
                </label>
                <label>
                  <input
                    type="radio"
                    checked={method === "PICKUP"}
                    onChange={() => setMethod("PICKUP")}
                  />
                  {t.pickup}
                </label>
              </fieldset>
              {method === "DELIVERY" && (
                <label>
                  {t.address}
                  <textarea
                    required
                    value={address}
                    onChange={(event) => setAddress(event.target.value)}
                    maxLength={1000}
                  />
                </label>
              )}
              {error && <p role="alert">{t.failed}</p>}
              <button className="public-primary" disabled={busy || lines.length === 0}>
                {busy ? "..." : t.place}
              </button>
            </form>
            <aside>
              <h2>{t.basket}</h2>
              {lines.map((product) => (
                <div className="public-cart-line" key={product.id}>
                  <span>
                    {product.name}
                    <small>{money(product.price)}</small>
                  </span>
                  <div>
                    <button
                      type="button"
                      onClick={() =>
                        setCart((current) => ({
                          ...current,
                          [product.id]: Math.max(0, current[product.id] - 1),
                        }))
                      }
                    >
                      −
                    </button>
                    {cart[product.id]}
                    <button
                      type="button"
                      onClick={() =>
                        setCart((current) => ({
                          ...current,
                          [product.id]: Math.min(product.stockQuantity, current[product.id] + 1),
                        }))
                      }
                    >
                      +
                    </button>
                  </div>
                </div>
              ))}
              <strong className="public-total">{money(total)}</strong>
            </aside>
          </div>
        </section>
      ) : (
        <>
          <section className="public-store-hero">
            <div className="public-store-avatar">
              {store.logoUrl ? <img src={store.logoUrl} alt="" /> : store.name.slice(0, 1)}
            </div>
            <p className="public-store-kicker">{t.tagline}</p>
            <h1>{store.name}</h1>
            <p>{store.description}</p>
            <small>{[store.city, store.region].filter(Boolean).join(", ")}</small>
            <div className="public-store-contact">
              {store.phone && (
                <a href={`tel:${store.phone}`}>
                  {t.call}: {store.phone}
                </a>
              )}
              {store.whatsapp && (
                <span>
                  {t.whatsapp}: {store.whatsapp}
                </span>
              )}
            </div>
          </section>
          <section className="public-catalogue">
            <div className="public-catalogue-heading">
              <div>
                <p className="public-store-kicker">{t.discover}</p>
                <h2>
                  {store.products.length} {t.products}
                </h2>
              </div>
              <input
                placeholder={t.search}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </div>
            {products.length ? (
              <div className="public-product-grid">
                {products.map((product) => (
                  <article className="public-product" key={product.id}>
                    <div className="public-product-image">
                      {product.imageUrl ? (
                        <img src={product.imageUrl} alt={product.name} loading="lazy" />
                      ) : (
                        <span>{product.name.slice(0, 1)}</span>
                      )}
                    </div>
                    <div className="public-product-copy">
                      <small>{product.category}</small>
                      <h3>{product.name}</h3>
                      <p>{product.description}</p>
                      <footer>
                        <strong>{money(product.price)}</strong>
                        <button
                          disabled={
                            product.stockQuantity < 1 ||
                            (cart[product.id] ?? 0) >= product.stockQuantity
                          }
                          onClick={() =>
                            setCart((current) => ({
                              ...current,
                              [product.id]: Math.min(
                                product.stockQuantity,
                                (current[product.id] ?? 0) + 1,
                              ),
                            }))
                          }
                        >
                          {product.stockQuantity < 1 ? t.soldOut : t.add}
                        </button>
                      </footer>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <p>{t.empty}</p>
            )}
          </section>
          {lines.length > 0 && (
            <aside className="public-cartbar">
              <strong>
                {lines.reduce((count, product) => count + cart[product.id], 0)} {t.items} ·{" "}
                {money(total)}
              </strong>
              <button onClick={() => setCheckout(true)}>{t.checkout} →</button>
            </aside>
          )}
        </>
      )}
      <footer className="public-store-footer">{t.footer}</footer>
    </main>
  );
}
