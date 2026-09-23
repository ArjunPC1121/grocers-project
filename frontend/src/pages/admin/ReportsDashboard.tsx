import { useEffect, useMemo, useState } from "react";
import { ArrowDownRight, ArrowUpRight, BarChart3, CalendarDays, ChevronRight, Package, RefreshCw, ShoppingBag, SlidersHorizontal, Users } from "lucide-react";
import api, { errorMessage } from "../../config/api";

type Period = "DAILY" | "WEEKLY" | "MONTHLY";
type Metric = "revenue" | "orders";
type Row = Record<string, unknown>;
type Report = { period: Period; fromDate: string; toDate: string; orderCount: number; revenue: number | string; orders: Row[] };
type Filters = { period: Period; referenceDate: string; productId: string; customerId: string };

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const today = new Date().toISOString().slice(0, 10);
const numberOf = (value: unknown) => Number(value ?? 0) || 0;
const textOf = (value: unknown) => String(value ?? "");
const dateOf = (value: unknown) => { const valueAsText = textOf(value); return valueAsText.length >= 10 ? valueAsText.slice(0, 10) : ""; };
const labelDate = (date: string) => new Intl.DateTimeFormat("en-IN", { month: "short", day: "numeric" }).format(new Date(`${date}T00:00:00`));

function addDays(date: string, days: number) { const next = new Date(`${date}T00:00:00`); next.setDate(next.getDate() + days); return next.toISOString().slice(0, 10); }
function startOfWeek(date: string) { const current = new Date(`${date}T00:00:00`); const offset = (current.getDay() + 6) % 7; current.setDate(current.getDate() - offset); return current.toISOString().slice(0, 10); }
function monthDays(date: string) { const current = new Date(`${date}T00:00:00`); return new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate(); }

function seriesFor(report: Report, metric: Metric) {
  const value = (order: Row) => metric === "revenue" ? numberOf(order.totalAmount) : 1;
  if (report.period === "DAILY") {
    const buckets = Array.from({ length: 24 }, (_, hour) => ({ key: String(hour).padStart(2, "0"), label: hour % 3 === 0 ? `${String(hour).padStart(2, "0")}:00` : "", value: 0 }));
    report.orders.forEach(order => { const hour = Number(textOf(order.orderedAt).slice(11, 13)); if (Number.isInteger(hour) && buckets[hour]) buckets[hour].value += value(order); });
    return buckets;
  }
  const start = report.period === "WEEKLY" ? startOfWeek(report.fromDate) : report.fromDate;
  const count = report.period === "WEEKLY" ? 7 : monthDays(report.fromDate);
  const buckets = Array.from({ length: count }, (_, index) => {
    const key = addDays(start, index);
    return { key, label: report.period === "WEEKLY" ? new Intl.DateTimeFormat("en", { weekday: "short" }).format(new Date(`${key}T00:00:00`)).slice(0, 1) : (index + 1) % 5 === 1 || index === 0 ? String(index + 1) : "", value: 0 };
  });
  report.orders.forEach(order => { const bucket = buckets.find(item => item.key === dateOf(order.orderedAt)); if (bucket) bucket.value += value(order); });
  return buckets;
}

function StatusPill({ status }: { status: string }) {
  const styles: Record<string, string> = { PLACED: "bg-sky-50 text-sky-700", SHIPPED: "bg-violet-50 text-violet-700", OUT_FOR_DELIVERY: "bg-amber-50 text-amber-700", DELIVERED: "bg-emerald-50 text-emerald-700" };
  return <span className={`rounded-full px-2.5 py-1 text-[11px] font-bold ${styles[status] || "bg-zinc-100 text-zinc-600"}`}>{status.replaceAll("_", " ")}</span>;
}

function MetricCard({ title, value, detail, icon: Icon, tone }: { title: string; value: string; detail: string; icon: typeof ShoppingBag; tone: string }) {
  return <article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-zinc-400">{title}</p><p className="mt-2 text-2xl font-bold text-app-text">{value}</p><p className="mt-1 text-xs text-zinc-500">{detail}</p></div><span className={`grid size-10 place-items-center rounded-xl ${tone}`}><Icon size={19}/></span></div></article>;
}

function RevenueChart({ data, metric }: { data: Array<{ key: string; label: string; value: number }>; metric: Metric }) {
  const high = Math.max(...data.map(item => item.value), 1);
  const total = data.reduce((sum, item) => sum + item.value, 0);
  return <div className="mt-6"><div className="flex items-end justify-between gap-4"><div><p className="font-bold text-app-text">{metric === "revenue" ? "Revenue trend" : "Order volume"}</p><p className="mt-1 text-sm text-zinc-500">{metric === "revenue" ? "Confirmed order value across the selected period." : "Confirmed orders across the selected period."}</p></div><b className="text-sm text-app-green">{metric === "revenue" ? currency.format(total) : `${total} orders`}</b></div><div className="mt-6 flex h-52 items-end gap-1.5 border-b border-l border-app-border px-3 pt-4 sm:gap-2">{data.map(item => <div key={item.key} className="group flex h-full min-w-0 flex-1 flex-col justify-end"><div title={`${item.key}: ${metric === "revenue" ? currency.format(item.value) : `${item.value} orders`}`} className="min-h-1 rounded-t-md bg-gradient-to-t from-app-green to-emerald-400 transition group-hover:from-app-orange group-hover:to-amber-300" style={{ height: `${Math.max(item.value ? (item.value / high) * 100 : 1, 1)}%` }}/><span className="mt-2 h-4 truncate text-center text-[10px] text-zinc-400">{item.label}</span></div>)}</div></div>;
}

export default function ReportsDashboard() {
  const [draft, setDraft] = useState<Filters>({ period: "WEEKLY", referenceDate: today, productId: "", customerId: "" });
  const [filters, setFilters] = useState<Filters>({ period: "WEEKLY", referenceDate: today, productId: "", customerId: "" });
  const [metric, setMetric] = useState<Metric>("revenue");
  const [report, setReport] = useState<Report | null>(null);
  const [products, setProducts] = useState<Row[]>([]);
  const [customers, setCustomers] = useState<Row[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => { Promise.all([api.get("/admin/products"), api.get("/admin/users")]).then(([productResponse, customerResponse]) => { setProducts(Array.isArray(productResponse.data) ? productResponse.data : []); setCustomers(Array.isArray(customerResponse.data) ? customerResponse.data : []); }).catch(() => undefined); }, []);
  useEffect(() => { setLoading(true); setError(""); api.get<Report>("/admin/reports", { params: { period: filters.period, referenceDate: filters.referenceDate, productId: filters.productId || undefined, customerId: filters.customerId || undefined } }).then(({ data }) => setReport({ ...data, orders: Array.isArray(data.orders) ? data.orders : [] })).catch(err => setError(errorMessage(err, "Unable to calculate this report."))).finally(() => setLoading(false)); }, [filters]);

  const data = useMemo(() => report ? seriesFor(report, metric) : [], [report, metric]);
  const metrics = useMemo(() => {
    const orders = report?.orders ?? [];
    const revenue = numberOf(report?.revenue);
    const average = orders.length ? revenue / orders.length : 0;
    const delivered = orders.filter(order => textOf(order.status) === "DELIVERED").length;
    return { revenue, average, delivered, orders: orders.length };
  }, [report]);
  const topProducts = useMemo(() => {
    const result = new Map<string, { name: string; units: number; revenue: number }>();
    report?.orders.forEach(order => { const items = Array.isArray(order.items) ? order.items : []; items.forEach(item => { if (!item || typeof item !== "object") return; const row = item as Row; const name = textOf(row.productName) || "Unnamed product"; const entry = result.get(name) || { name, units: 0, revenue: 0 }; entry.units += numberOf(row.quantity); entry.revenue += numberOf(row.subtotal); result.set(name, entry); }); });
    return [...result.values()].sort((left, right) => right.revenue - left.revenue).slice(0, 5);
  }, [report]);
  const statusGroups = useMemo(() => {
    const counts = new Map<string, number>(); report?.orders.forEach(order => { const status = textOf(order.status) || "UNKNOWN"; counts.set(status, (counts.get(status) || 0) + 1); }); return [...counts.entries()].sort((left, right) => right[1] - left[1]);
  }, [report]);
  const range = report ? `${labelDate(report.fromDate)} – ${labelDate(report.toDate)}` : "Loading range…";
  const apply = () => setFilters({ ...draft });
  const reset = () => { const next = { period: "WEEKLY" as Period, referenceDate: today, productId: "", customerId: "" }; setDraft(next); setFilters(next); };

  return <section className="animate-fade-in pb-8"><div className="flex flex-wrap items-end justify-between gap-5"><div><span className="rounded-full bg-orange-50 px-3 py-1 text-xs font-bold text-app-orange">REPORTING</span><h2 className="mt-4 font-serif text-4xl text-app-text">Sales reports</h2><p className="mt-2 text-zinc-500">Choose a period or narrow the view to a product or user. Revenue includes only confirmed, paid orders.</p></div><button onClick={() => setFilters({ ...filters })} className="inline-flex items-center gap-2 rounded-xl border border-app-border bg-white px-4 py-2.5 text-sm font-semibold shadow-sm hover:bg-app-cream"><RefreshCw size={16}/>Refresh</button></div>

    <div className="mt-7 rounded-2xl border border-app-border/80 bg-white p-4 shadow-sm sm:p-5"><div className="flex items-center gap-2 text-sm font-bold text-app-green"><SlidersHorizontal size={17}/>Report controls</div><div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-[150px_180px_1fr_1fr_auto]"><label className="text-xs font-bold uppercase tracking-wide text-zinc-500">Period<select value={draft.period} onChange={event => setDraft(current => ({ ...current, period: event.target.value as Period }))} className="mt-1.5 w-full rounded-xl border border-app-border bg-white p-3 text-sm font-medium text-app-text"><option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option></select></label><label className="text-xs font-bold uppercase tracking-wide text-zinc-500">Reference date<input type="date" value={draft.referenceDate} onChange={event => setDraft(current => ({ ...current, referenceDate: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-app-border bg-white p-3 text-sm font-medium text-app-text"/></label><label className="text-xs font-bold uppercase tracking-wide text-zinc-500">Product<select value={draft.productId} onChange={event => setDraft(current => ({ ...current, productId: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-app-border bg-white p-3 text-sm font-medium text-app-text"><option value="">All products</option>{products.map(product => <option key={textOf(product.id)} value={textOf(product.id)}>{textOf(product.name)}</option>)}</select></label><label className="text-xs font-bold uppercase tracking-wide text-zinc-500">User<select value={draft.customerId} onChange={event => setDraft(current => ({ ...current, customerId: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-app-border bg-white p-3 text-sm font-medium text-app-text"><option value="">All users</option>{customers.map(customer => <option key={textOf(customer.id)} value={textOf(customer.id)}>{`${textOf(customer.firstName)} ${textOf(customer.lastName)}`.trim() || textOf(customer.email)}</option>)}</select></label><div className="flex items-end gap-2"><button onClick={reset} className="rounded-xl border border-app-border px-3 py-3 text-sm font-semibold text-zinc-600">Reset</button><button onClick={apply} className="rounded-xl bg-app-orange px-5 py-3 text-sm font-bold text-white shadow-lg shadow-orange-100">Apply</button></div></div></div>

    {error ? <div className="mt-6 rounded-2xl border border-rose-100 bg-rose-50 p-5 text-sm text-rose-700">{error}</div> : loading ? <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">{[1, 2, 3, 4].map(item => <div key={item} className="h-32 animate-pulse rounded-2xl bg-white"/>)}</div> : <><div className="mt-6 flex flex-wrap items-center justify-between gap-3"><div className="inline-flex rounded-xl border border-app-border bg-white p-1"><button onClick={() => setMetric("revenue")} className={`rounded-lg px-4 py-2 text-sm font-bold ${metric === "revenue" ? "bg-app-green text-white" : "text-zinc-500"}`}>Revenue</button><button onClick={() => setMetric("orders")} className={`rounded-lg px-4 py-2 text-sm font-bold ${metric === "orders" ? "bg-app-green text-white" : "text-zinc-500"}`}>Orders</button></div><p className="inline-flex items-center gap-2 text-sm font-medium text-zinc-500"><CalendarDays size={16}/>{range}</p></div>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><MetricCard title="Confirmed revenue" value={currency.format(metrics.revenue)} detail={`${metrics.orders} confirmed orders`} icon={BarChart3} tone="bg-emerald-50 text-emerald-700"/><MetricCard title="Average order" value={currency.format(metrics.average)} detail="Revenue divided by confirmed orders" icon={ShoppingBag} tone="bg-orange-50 text-app-orange"/><MetricCard title="Delivered" value={String(metrics.delivered)} detail="Orders completed in this period" icon={Package} tone="bg-sky-50 text-sky-700"/><MetricCard title="Fulfilment rate" value={metrics.orders ? `${Math.round((metrics.delivered / metrics.orders) * 100)}%` : "—"} detail="Delivered share of confirmed orders" icon={Users} tone="bg-violet-50 text-violet-700"/></div>
      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(280px,.8fr)]"><article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><RevenueChart data={data} metric={metric}/></article><article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><p className="font-bold text-app-text">Order progress</p><p className="mt-1 text-sm text-zinc-500">Confirmed orders by fulfilment stage.</p><div className="mt-6 space-y-4">{statusGroups.length ? statusGroups.map(([status, count]) => <div key={status}><div className="flex items-center justify-between gap-3"><StatusPill status={status}/><b className="text-sm">{count}</b></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-app-cream"><div className="h-full rounded-full bg-app-green" style={{ width: `${metrics.orders ? (count / metrics.orders) * 100 : 0}%` }}/></div></div>) : <p className="rounded-xl bg-app-cream p-4 text-sm text-zinc-500">No confirmed orders in this period.</p>}</div></article></div>
      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)]"><article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><div><p className="font-bold">Top products</p><p className="mt-1 text-sm text-zinc-500">Ranked by confirmed sales value.</p></div><Package className="text-app-orange" size={20}/></div><div className="mt-5 space-y-4">{topProducts.length ? topProducts.map((product, index) => <div key={product.name} className="flex items-center gap-3"><span className="grid size-8 shrink-0 place-items-center rounded-lg bg-app-cream text-xs font-bold text-app-green">{index + 1}</span><div className="min-w-0 flex-1"><div className="flex justify-between gap-3"><b className="truncate text-sm">{product.name}</b><b className="shrink-0 text-sm text-app-green">{currency.format(product.revenue)}</b></div><p className="mt-1 text-xs text-zinc-500">{product.units} unit{product.units === 1 ? "" : "s"} sold</p></div></div>) : <p className="rounded-xl bg-app-cream p-4 text-sm text-zinc-500">Item-level data is not available for these orders.</p>}</div></article><article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><div><p className="font-bold">Recent confirmed orders</p><p className="mt-1 text-sm text-zinc-500">Newest orders matching the current report.</p></div><ShoppingBag className="text-app-orange" size={20}/></div><div className="mt-4 divide-y divide-app-border/60">{report?.orders.slice(0, 5).map(order => <div key={textOf(order.id) || textOf(order.orderNumber)} className="flex items-center justify-between gap-3 py-3"><div className="min-w-0"><b className="block truncate text-sm">{textOf(order.orderNumber) || `Order #${textOf(order.id)}`}</b><span className="mt-1 block text-xs text-zinc-500">{labelDate(dateOf(order.orderedAt))} · User #{textOf(order.customerId)}</span></div><div className="flex shrink-0 items-center gap-3"><b className="text-sm">{currency.format(numberOf(order.totalAmount))}</b><ChevronRight size={16} className="text-zinc-400"/></div></div>)}{!report?.orders.length && <p className="rounded-xl bg-app-cream p-4 text-sm text-zinc-500">No confirmed orders match these controls.</p>}</div></article></div></>}</section>;
}
