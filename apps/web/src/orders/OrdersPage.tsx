import { useCallback, useEffect, useState } from "react";
import { useApi } from "../auth/api";
import { nextFulfilmentStatuses, orderStatusLabel } from "./orderStatus";
import "./orders.css";

type Item = {
  name: string;
  unitPrice: number;
  quantity: number;
  total: number;
};

type Order = {
  id: string;
  reference: string;
  customerName: string;
  phone: string;
  fulfilmentMethod: string;
  deliveryAddress: string | null;
  note: string | null;
  status: string;
  fulfilmentStatus: string;
  total: number;
  createdAt: string;
  items: Item[];
};

type Props = {
  businessId: string;
  language: "en" | "fr";
};

const words = {
  en: {
    title: "Orders",
    intro: "Review new customer orders and keep fulfilment moving.",
    empty: "No orders yet. New customer orders will appear here.",
    loading: "Loading orders...",
    accept: "Accept order",
    reject: "Reject",
    preparing: "Preparing",
    ready: "Ready for pickup",
    dispatched: "Dispatched",
    delivered: "Delivered",
    completed: "Completed",
    delivery: "Delivery",
    pickup: "Pickup",
    failed: "Could not load or update orders.",
  },
  fr: {
    title: "Commandes",
    intro: "Consultez les commandes et suivez leur préparation.",
    empty: "Aucune commande pour le moment.",
    loading: "Chargement des commandes...",
    accept: "Accepter",
    reject: "Refuser",
    preparing: "Préparation",
    ready: "Prête au retrait",
    dispatched: "Expédiée",
    delivered: "Livrée",
    completed: "Terminée",
    delivery: "Livraison",
    pickup: "Retrait",
    failed: "Impossible de charger ou modifier les commandes.",
  },
} as const;

const money = (value: number) => new Intl.NumberFormat("fr-CM").format(value) + " FCFA";

export default function OrdersPage({ businessId, language }: Props) {
  const api = useApi();
  const t = words[language];
  const [orders, setOrders] = useState<Order[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api(`/api/v1/businesses/${businessId}/orders`);
      if (!response.ok) throw new Error();
      setOrders((await response.json()) as Order[]);
    } catch {
      setError(t.failed);
    } finally {
      setLoading(false);
    }
  }, [api, businessId, t.failed]);

  useEffect(() => {
    void load();
  }, [load]);

  async function decide(order: Order, action: "ACCEPT" | "REJECT") {
    setBusy(order.id);
    setError("");
    try {
      const response = await api(
        `/api/v1/businesses/${businessId}/orders/${order.id}/decision?action=${action}`,
        { method: "POST" },
      );
      if (!response.ok) throw new Error();
      await load();
    } catch {
      setError(t.failed);
    } finally {
      setBusy(null);
    }
  }

  async function update(order: Order, status: string) {
    setBusy(order.id);
    setError("");
    try {
      const response = await api(
        `/api/v1/businesses/${businessId}/orders/${order.id}/fulfilment?status=${status}`,
        { method: "PATCH" },
      );
      if (!response.ok) throw new Error();
      await load();
    } catch {
      setError(t.failed);
    } finally {
      setBusy(null);
    }
  }

  return (
    <section className="orders-page">
      <header>
        <div>
          <p className="eyebrow">{t.title}</p>
          <h2>{t.title}</h2>
          <p>{t.intro}</p>
        </div>
        <span className="orders-count">{orders.length}</span>
      </header>
      {error && (
        <p role="alert" className="orders-error">
          {error}
        </p>
      )}
      {loading ? (
        <p className="orders-empty">{t.loading}</p>
      ) : orders.length === 0 && !error ? (
        <p className="orders-empty">{t.empty}</p>
      ) : (
        <div className="orders-list">
          {orders.map((order) => (
            <article className="vendor-order" key={order.id}>
              <div className="vendor-order-heading">
                <div>
                  <strong>{order.reference}</strong>
                  <small>
                    {new Date(order.createdAt).toLocaleString(
                      language === "fr" ? "fr-FR" : "en-GB",
                    )}
                  </small>
                </div>
                <span className={`order-status status-${order.status.toLowerCase()}`}>
                  {orderStatusLabel(language, order.status)}
                </span>
              </div>
              <div className="vendor-order-customer">
                <strong>{order.customerName}</strong>
                <a href={`tel:${order.phone}`}>{order.phone}</a>
                <span>{order.fulfilmentMethod === "DELIVERY" ? t.delivery : t.pickup}</span>
              </div>
              {order.deliveryAddress && (
                <p className="vendor-order-address">{order.deliveryAddress}</p>
              )}
              <div className="vendor-order-items">
                {order.items.map((item, index) => (
                  <div key={index}>
                    <span>
                      {item.quantity} × {item.name}
                    </span>
                    <strong>{money(item.total)}</strong>
                  </div>
                ))}
              </div>
              <div className="vendor-order-total">
                <span>Total</span>
                <strong>{money(order.total)}</strong>
              </div>
              {order.note && <p className="vendor-order-note">{order.note}</p>}
              <footer>
                {order.status === "NEW" ? (
                  <>
                    <button
                      disabled={busy === order.id}
                      onClick={() => void decide(order, "ACCEPT")}
                    >
                      {t.accept}
                    </button>
                    <button
                      className="order-reject"
                      disabled={busy === order.id}
                      onClick={() => void decide(order, "REJECT")}
                    >
                      {t.reject}
                    </button>
                  </>
                ) : order.status === "ACCEPTED" &&
                  nextFulfilmentStatuses(order.fulfilmentMethod, order.fulfilmentStatus).length >
                    0 ? (
                  <select
                    disabled={busy === order.id}
                    value={order.fulfilmentStatus}
                    onChange={(event) => void update(order, event.target.value)}
                  >
                    <option value={order.fulfilmentStatus}>
                      {orderStatusLabel(language, order.fulfilmentStatus)}
                    </option>
                    {nextFulfilmentStatuses(order.fulfilmentMethod, order.fulfilmentStatus).map(
                      (status) => (
                        <option key={status} value={status}>
                          {orderStatusLabel(language, status)}
                        </option>
                      ),
                    )}
                  </select>
                ) : (
                  <span>{orderStatusLabel(language, order.fulfilmentStatus)}</span>
                )}
              </footer>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
