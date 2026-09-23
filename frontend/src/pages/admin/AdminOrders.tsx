import { useEffect, useMemo, useState } from "react";
import { Search, ShoppingBag } from "lucide-react";
import api, { errorMessage } from "../../config/api";

type Order = {
  id: number;
  orderNumber?: string;
  customerId?: number;
  paymentMethod?: string;
  totalAmount?: number | string;
  status?: string;
  orderedAt?: string;
  items?: unknown[];
};

const label = (value?: string) =>
  (value || "Unknown")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());

const amount = (value?: number | string) => {
  const number = Number(value);
  return Number.isFinite(number)
    ? new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(number)
    : "—";
};

const placedOn = (value?: string) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? "—"
    : new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeStyle: "short" }).format(date);
};

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .get<Order[]>("/orders")
      .then(({ data }) => {
        const rows = Array.isArray(data) ? data : [];
        setOrders(rows.sort((a, b) => new Date(b.orderedAt || 0).valueOf() - new Date(a.orderedAt || 0).valueOf()));
      })
      .catch((requestError) => setError(errorMessage(requestError, "Could not load orders.")))
      .finally(() => setLoading(false));
  }, []);

  const visibleOrders = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return orders;
    return orders.filter((order) =>
      [order.orderNumber, order.id, order.customerId, order.status, order.paymentMethod]
        .filter((value) => value !== undefined && value !== null)
        .join(" ")
        .toLowerCase()
        .includes(term),
    );
  }, [orders, query]);

  return (
    <section className="animate-fade-in">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-app-orange">Store activity</p>
          <h1 className="mt-1 font-serif text-4xl text-app-text">Orders</h1>
          <p className="mt-2 text-sm text-zinc-500">Every order placed in your store, in one simple list.</p>
        </div>
        <span className="rounded-xl bg-app-cream px-4 py-2 text-sm font-bold text-app-green">{orders.length} total orders</span>
      </div>

      <label className="mt-6 flex max-w-xl items-center gap-3 rounded-xl border border-app-border bg-white px-4 py-3 text-zinc-400 shadow-sm">
        <Search size={18} />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className="w-full bg-transparent text-sm text-app-text outline-none"
          placeholder="Search by order number, customer, status or payment method"
        />
      </label>

      {error && <p className="mt-5 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}

      <div className="mt-5 overflow-hidden rounded-2xl border border-app-border bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-[850px] w-full text-left text-sm">
            <thead className="border-b border-app-border bg-app-cream/60 text-xs font-bold uppercase tracking-wide text-zinc-500">
              <tr>
                <th className="px-5 py-4">Order</th>
                <th className="px-5 py-4">Customer</th>
                <th className="px-5 py-4">Items</th>
                <th className="px-5 py-4">Payment</th>
                <th className="px-5 py-4">Status</th>
                <th className="px-5 py-4">Total</th>
                <th className="px-5 py-4">Placed</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-app-border">
              {loading && (
                <tr><td colSpan={7} className="px-5 py-12 text-center text-zinc-500">Loading orders…</td></tr>
              )}
              {!loading && visibleOrders.map((order) => (
                <tr key={order.id} className="transition-colors hover:bg-app-cream/40">
                  <td className="px-5 py-4"><span className="font-bold text-app-text">{order.orderNumber || `#${order.id}`}</span></td>
                  <td className="px-5 py-4 text-zinc-600">#{order.customerId ?? "—"}</td>
                  <td className="px-5 py-4 text-zinc-600">{order.items?.length ?? 0}</td>
                  <td className="px-5 py-4 text-zinc-600">{label(order.paymentMethod)}</td>
                  <td className="px-5 py-4"><span className="rounded-full bg-app-cream px-3 py-1 text-xs font-bold text-app-green">{label(order.status)}</span></td>
                  <td className="px-5 py-4 font-bold text-app-text">{amount(order.totalAmount)}</td>
                  <td className="px-5 py-4 text-zinc-600">{placedOn(order.orderedAt)}</td>
                </tr>
              ))}
              {!loading && !visibleOrders.length && (
                <tr>
                  <td colSpan={7} className="px-5 py-14 text-center text-zinc-500">
                    <ShoppingBag className="mx-auto mb-3 text-zinc-300" size={28} />
                    No orders match this search.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
