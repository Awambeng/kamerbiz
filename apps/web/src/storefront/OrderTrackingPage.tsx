import { useEffect, useState } from "react";
import "./storefront.css";
import { orderStatusLabel } from "../orders/orderStatus";

type Tracking = {
  reference: string;
  status: string;
  fulfilmentStatus: string;
};

export default function OrderTrackingPage() {
  const token = window.location.pathname.split("/").filter(Boolean)[1] ?? "";
  const [data, setData] = useState<Tracking | null>(null);
  const [language, setLanguage] = useState<"en" | "fr">("en");
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    fetch(`/api/v1/public/orders/track/${encodeURIComponent(token)}`)
      .then(async (response) => {
        if (!response.ok) throw new Error();
        return response.json() as Promise<Tracking>;
      })
      .then(setData)
      .catch(() => setFailed(true));
  }, [token]);

  const fr = language === "fr";
  const footer = fr
    ? "KamerBiz · Des entreprises plus fortes. Un Cameroun plus fort."
    : "KamerBiz · Stronger businesses. A stronger Cameroon.";

  return (
    <main className="public-store">
      <header className="public-store-header">
        <a className="public-store-brand" href="/">
          <span>K</span>
          KamerBiz
        </a>
        <div>
          <button className={!fr ? "chosen" : ""} onClick={() => setLanguage("en")}>
            EN
          </button>
          <button className={fr ? "chosen" : ""} onClick={() => setLanguage("fr")}>
            FR
          </button>
        </div>
      </header>
      <section className="public-store-success">
        <span>✓</span>
        <p className="public-store-kicker">{fr ? "SUIVI DE COMMANDE" : "ORDER TRACKING"}</p>
        {failed ? (
          <h1>
            {fr
              ? "Lien de suivi invalide ou expiré."
              : "This tracking link is invalid or unavailable."}
          </h1>
        ) : data ? (
          <>
            <h1>{data.reference}</h1>
            <div className="tracking-line">
              <span>{fr ? "Commande" : "Order"}</span>
              <strong>{orderStatusLabel(language, data.status)}</strong>
            </div>
            <div className="tracking-line">
              <span>{fr ? "Livraison" : "Fulfilment"}</span>
              <strong>{orderStatusLabel(language, data.fulfilmentStatus)}</strong>
            </div>
            <p>
              {fr
                ? "Le vendeur mettra à jour le suivi au fur et à mesure."
                : "The vendor will update your order progress here."}
            </p>
          </>
        ) : (
          <p>{fr ? "Chargement..." : "Loading..."}</p>
        )}
      </section>
      <footer className="public-store-footer">{footer}</footer>
    </main>
  );
}
