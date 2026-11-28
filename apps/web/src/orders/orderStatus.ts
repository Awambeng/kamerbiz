export type OrderLanguage = "en" | "fr";

const labels = {
  en: {
    NEW: "New",
    ACCEPTED: "Accepted",
    REJECTED: "Rejected",
    COMPLETED: "Completed",
    NOT_ARRANGED: "Not arranged",
    PREPARING: "Preparing",
    READY_FOR_PICKUP: "Ready for pickup",
    DISPATCHED: "Dispatched",
    DELIVERED: "Delivered",
  },
  fr: {
    NEW: "Nouvelle",
    ACCEPTED: "Acceptée",
    REJECTED: "Refusée",
    COMPLETED: "Terminée",
    NOT_ARRANGED: "Non planifiée",
    PREPARING: "En préparation",
    READY_FOR_PICKUP: "Prête au retrait",
    DISPATCHED: "Expédiée",
    DELIVERED: "Livrée",
  },
} as const;

export function orderStatusLabel(language: OrderLanguage, value: string): string {
  const dictionary: Record<string, string> = labels[language];
  return dictionary[value] ?? value.replaceAll("_", " ").toLowerCase();
}

export function nextFulfilmentStatuses(method: string, current: string): string[] {
  if (current === "PREPARING") {
    return method === "PICKUP" ? ["READY_FOR_PICKUP"] : ["DISPATCHED"];
  }
  if (current === "READY_FOR_PICKUP" && method === "PICKUP") return ["COMPLETED"];
  if (current === "DISPATCHED" && method === "DELIVERY") return ["DELIVERED"];
  return [];
}
