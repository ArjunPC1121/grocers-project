import { useEffect, useMemo, useState } from "react";
import { BarChart3, ChevronRight, CirclePlus, ClipboardCheck, Package, PackageSearch, RefreshCw, ShoppingBag, Sparkles, Users } from "lucide-react";
import { useNavigate } from "react-router-dom";
import api, { errorMessage } from "../../config/api";

type Row = Record<string, unknown>;
type Range = "week" | "month" | "quarter";
type Dashboard = {
  totalProducts: number;
  lowStockProducts: number;
  activeEmployees: number;
  pendingRequests: number;
  totalOrders: number;
  revenue: number | string;
  orderStatuses: Record<string, number>;
  recentOrders: Row[];
  lowStockItems: Row[];
};
type Report = { revenue: number | string; orders: Row[] };

const today = new Date().toISOString().slice(0, 10);
const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const text = (value: unknown) => String(value ?? "");
const number = (value: unknown) => Number(value ?? 0) || 0;
const title = (status: string) => status.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
const dateLabel = (value: unknown) => {
  const date = new Date(text(value));
  return Number.isNaN(date.getTime()) ? "Recently" : new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" }).format(date);
};

function shiftMonth(offset: number) {
  const date = new Date(`${today}T00:00:00`);
  date.setMonth(date.getMonth() + offset);
  return date.toISOString().slice(0, 10);
}

function shiftDays(offset: number) {
  const date = new Date(`${today}T00:00:00`);
  date.setDate(date.getDate() + offset);
  return date.toISOString().slice(0, 10);
}

function requestFor(range: Range, offset = 0) {
  if (range === "week") return api.get<Report>("/admin/reports", { params: { period: "WEEKLY", referenceDate: shiftDays(offset * 7) } });
  return api.get<Report>("/admin/reports", { params: { period: "MONTHLY", referenceDate: shiftMonth(offset) } });
}

function StatusPill({ status }: { status: string }) {
  const styles: Record<string, string> = { DELIVERED: "bg-emerald-50 text-emerald-700", OUT_FOR_DELIVERY: "bg-amber-50 text-amber-700", SHIPPED: "bg-violet-50 text-violet-700", PLACED: "bg-sky-50 text-sky-700", CREATED: "bg-zinc-100 text-zinc-700", CANCELLED: "bg-rose-50 text-rose-700" };
  return <span className={`rounded-full px-2.5 py-1 text-[11px] font-bold ${styles[status] || "bg-zinc-100 text-zinc-600"}`}>{title(status)}</span>;
}

function MetricCard({ label, value, detail, Icon, tone }: { label: string; value: string; detail: string; Icon: typeof BarChart3; tone: string }) {
  return <article className="rounded-2xl border border-app-border/70 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-zinc-400">{label}</p><p className="mt-2 text-2xl font-bold">{value}</p><p className="mt-1 text-xs text-zinc-500">{detail}</p></div><span className={`grid size-10 place-items-center rounded-xl ${tone}`}><Icon size={19}/></span></div></article>;
}

function SalesAnalytics({ range, onRange, reports, previousRevenue }: { range: Range; onRange: (range: Range) => void; reports: Report[]; previousRevenue: number }) {
  const series = useMemo(() => {
    if (range === "quarter") return reports.slice().reverse().map((report, index) => ({ label: new Intl.DateTimeFormat("en", { month: "short" }).format(new Date(`${shiftMonth(index - 2)}T00:00:00`)), value: number(report.revenue) }));
    const days = range === "week" ? 7 : new Date(new Date(`${today}T00:00:00`).getFullYear(), new Date(`${today}T00:00:00`).getMonth() + 1, 0).getDate();
    const start = new Date(`${today}T00:00:00`);
    start.setDate(range === "week" ? start.getDate() - ((start.getDay() + 6) % 7) : 1);
    const buckets = Array.from({ length: days }, (_, index) => { const date = new Date(start); date.setDate(date.getDate() + index); return { key: date.toISOString().slice(0, 10), label: range === "week" ? new Intl.DateTimeFormat("en", { weekday: "short" }).format(date) : index === 0 || (index + 1) % 5 === 0 ? String(index + 1) : "", value: 0 }; });
    reports[0]?.orders.forEach(order => { const bucket = buckets.find(item => item.key === text(order.orderedAt).slice(0, 10)); if (bucket) bucket.value += number(order.totalAmount); });
    return buckets;
  }, [range, reports]);
  const revenue = reports.reduce((sum, report) => sum + number(report.revenue), 0);
  const change = previousRevenue ? ((revenue - previousRevenue) / previousRevenue) * 100 : null;
  const highest = Math.max(...series.map(item => item.value), 1);
  return <article className="rounded-3xl border border-app-border/70 bg-white p-5 shadow-sm sm:p-6"><div className="flex flex-wrap items-start justify-between gap-4"><div><h3 className="text-lg font-bold">Sales analytics</h3><p className="mt-1 text-sm text-zinc-500">{change === null ? "No earlier period is available for comparison." : <>{change >= 0 ? "Revenue is up " : "Revenue is down "}<b className={change >= 0 ? "text-emerald-600" : "text-rose-600"}>{Math.abs(change).toFixed(1)}%</b> compared with the previous period.</>}</p></div><div className="flex rounded-xl bg-app-cream p-1">{(["week", "month", "quarter"] as Range[]).map(item => <button key={item} onClick={() => onRange(item)} className={`rounded-lg px-3 py-1.5 text-xs font-semibold ${range === item ? "bg-white text-app-green shadow-sm" : "text-zinc-500"}`}>{item === "week" ? "This week" : item[0].toUpperCase() + item.slice(1)}</button>)}</div></div><div className="mt-6 flex h-56 items-end gap-1.5 border-b border-l border-app-border px-3 pt-4 sm:gap-2">{series.map(item => <div key={item.key || item.label} className="group flex h-full min-w-0 flex-1 flex-col justify-end"><div title={`${item.label}: ${currency.format(item.value)}`} className="min-h-1 rounded-t-md bg-gradient-to-t from-app-orange to-amber-300 transition group-hover:from-app-green group-hover:to-emerald-400" style={{ height: `${Math.max((item.value / highest) * 100, 1)}%` }}/><span className="mt-2 h-4 truncate text-center text-[10px] text-zinc-400">{item.label}</span></div>)}</div></article>;
}

function OrderStatus({ statuses, total }: { statuses: Record<string, number>; total: number }) {
  const palette = ["#1b3022", "#f97316", "#fbbf24", "#8b5cf6", "#fb7185", "#38bdf8"];
  const entries = Object.entries(statuses).sort((left, right) => right[1] - left[1]);
  let current = 0;
  const stops = entries.map(([_, count], index) => { const from = current; current += total ? (count / total) * 100 : 0; return `${palette[index % palette.length]} ${from}% ${current}%`; }).join(", ");
  return <article className="rounded-3xl border border-app-border/70 bg-white p-6 shadow-sm"><h3 className="text-lg font-bold">Order status</h3><p className="text-sm text-zinc-500">Live status across all orders</p><div className="relative mx-auto mt-6 grid size-44 place-items-center rounded-full" style={{ background: stops ? `conic-gradient(${stops})` : "#f4f4f5" }}><div className="grid size-28 place-items-center rounded-full bg-white text-center"><b className="text-3xl">{total}</b><span className="text-xs text-zinc-400">Total orders</span></div></div><div className="mt-6 space-y-3">{entries.length ? entries.map(([status, count], index) => <div key={status} className="flex items-center gap-2 text-sm"><i className="size-2.5 rounded-full" style={{ background: palette[index % palette.length] }}/><span className="min-w-0 flex-1 truncate text-zinc-500">{title(status)}</span><b>{count}</b><span className="w-10 text-right text-xs text-zinc-400">{total ? Math.round((count / total) * 100) : 0}%</span></div>) : <p className="rounded-xl bg-app-cream p-4 text-sm text-zinc-500">No orders yet.</p>}</div></article>;
}

function RecentOrders({ orders }: { orders: Row[] }) {
  const navigate = useNavigate();
  return <article className="overflow-hidden rounded-3xl border border-app-border/70 bg-white shadow-sm"><div className="flex items-center justify-between p-5 sm:p-6"><div><h3 className="text-lg font-bold">Recent orders</h3><p className="text-sm text-zinc-500">Latest orders from your storefront</p></div><button onClick={() => navigate("/admin/reports")} className="rounded-xl bg-app-cream px-3 py-2 text-xs font-bold text-app-green">View reports <ChevronRight className="inline" size={14}/></button></div><div className="overflow-x-auto"><table className="w-full min-w-[670px] text-left text-sm"><thead className="border-y border-app-border/60 bg-app-cream/60 text-[11px] uppercase tracking-wider text-zinc-400"><tr>{["Order", "User", "Items", "Payment", "Status", "Amount"].map(label => <th key={label} className="px-5 py-3">{label}</th>)}</tr></thead><tbody>{orders.map(order => <tr key={text(order.id)} className="border-b border-app-border/60 hover:bg-orange-50/40"><td className="px-5 py-4 font-semibold text-app-green">{text(order.orderNumber) || `#${text(order.id)}`}<span className="mt-1 block text-xs font-normal text-zinc-400">{dateLabel(order.orderedAt)}</span></td><td className="px-5 py-4 text-zinc-600">User #{text(order.customerId) || "—"}</td><td className="px-5 py-4 text-zinc-500">{Array.isArray(order.items) ? `${order.items.length} item${order.items.length === 1 ? "" : "s"}` : "—"}</td><td className="px-5 py-4"><span className="rounded-full bg-green-50 px-2.5 py-1 text-xs font-semibold text-green-700">{title(text(order.paymentMethod) || "Not recorded")}</span></td><td className="px-5 py-4"><StatusPill status={text(order.status) || "CREATED"}/></td><td className="px-5 py-4 font-bold">{currency.format(number(order.totalAmount))}</td></tr>)}{!orders.length && <tr><td colSpan={6} className="px-5 py-10 text-center text-zinc-500">No orders have been placed yet.</td></tr>}</tbody></table></div></article>;
}

function LowStock({ products }: { products: Row[] }) {
  const navigate = useNavigate();
  return <article className="rounded-3xl border border-app-border/70 bg-white p-5 shadow-sm sm:p-6"><span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-rose-600"><PackageSearch size={11}/>{products.length} low</span><h3 className="mt-3 text-lg font-bold">Low stock alerts</h3><p className="text-sm text-zinc-500">Products at 10 units or fewer</p><div className="mt-5 space-y-4">{products.map(product => <div key={text(product.id)} className="flex items-center gap-3"><div className="grid size-10 shrink-0 place-items-center overflow-hidden rounded-xl bg-app-cream text-app-orange">{text(product.imageUrl) ? <img src={text(product.imageUrl)} alt="" className="size-full object-cover"/> : <Package size={18}/>}</div><div className="min-w-0 flex-1"><div className="flex justify-between gap-3 text-sm"><b className="truncate">{text(product.name) || "Unnamed product"}</b><b className="shrink-0 text-rose-500">{number(product.quantity)} left</b></div><p className="truncate text-xs text-zinc-400">{text(product.category) || "Uncategorised"}</p></div></div>)}{!products.length && <p className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700">All products have sufficient stock.</p>}</div><button onClick={() => navigate("/admin/products")} className="mt-5 w-full rounded-xl border border-app-green/15 py-2.5 text-sm font-bold text-app-green hover:bg-app-green hover:text-white">Manage products</button></article>;
}

function Highlights() {
  const navigate = useNavigate();
  const actions = [[CirclePlus, "Create product", "Add something new", "/admin/products"], [Package, "Launch an offer", "Set a product discount", "/admin/products"], [ClipboardCheck, "Review requests", "Approve team changes", "/admin/requests"]] as const;
  return <article className="rounded-3xl bg-app-green p-5 text-white shadow-xl sm:p-6"><div className="flex items-center gap-2"><Sparkles className="text-amber-300" size={20}/><h3 className="text-lg font-bold">Today’s highlights</h3></div><p className="mt-1 text-sm text-white/60">Useful next actions for your store.</p><div className="mt-5 grid gap-3 md:grid-cols-3">{actions.map(([Icon, heading, detail, path]) => <button onClick={() => navigate(path)} key={heading} className="group flex items-center gap-3 rounded-2xl bg-white/10 p-4 text-left hover:-translate-y-1 hover:bg-white hover:text-app-green"><Icon className="text-orange-300 group-hover:text-app-orange" size={22}/><span><b className="block text-sm">{heading}</b><small className="text-white/60 group-hover:text-zinc-500">{detail}</small></span><ChevronRight className="ml-auto opacity-50" size={16}/></button>)}</div></article>;
}

export default function LiveAdminDashboard() {
  const [range, setRange] = useState<Range>("week");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [previousRevenue, setPreviousRevenue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true); setError("");
    try {
      const currentOffsets = range === "quarter" ? [0, -1, -2] : [0];
      const previousOffsets = range === "quarter" ? [-3, -4, -5] : [-1];
      const [dashboardResponse, current, previous] = await Promise.all([
        api.get<Dashboard>("/admin/dashboard"),
        Promise.all(currentOffsets.map(offset => requestFor(range, offset))),
        Promise.all(previousOffsets.map(offset => requestFor(range, offset)))
      ]);
      setDashboard(dashboardResponse.data);
      setReports(current.map(response => ({ ...response.data, orders: Array.isArray(response.data.orders) ? response.data.orders : [] })));
      setPreviousRevenue(previous.reduce((sum, response) => sum + number(response.data.revenue), 0));
    } catch (requestError) { setError(errorMessage(requestError, "Live dashboard data is unavailable.")); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); const interval = window.setInterval(load, 30000); return () => window.clearInterval(interval); }, [range]);
  const revenue = number(dashboard?.revenue);
  return <section className="animate-fade-in space-y-6"><div className="flex flex-wrap items-end justify-between gap-4"><div><p className="text-sm text-zinc-500">A live view of today’s store activity.</p><h2 className="mt-1 font-serif text-4xl">Your grocery pulse <span className="text-app-orange">✦</span></h2></div><button onClick={load} disabled={loading} className="inline-flex items-center gap-2 rounded-xl border border-app-border bg-white px-4 py-2.5 text-sm font-semibold shadow-sm hover:bg-app-cream disabled:opacity-60"><RefreshCw className={loading ? "animate-spin" : ""} size={16}/>Refresh</button></div>{error && <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"><b>Live dashboard data could not be loaded.</b> {error}</div>}<div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5"><MetricCard label="Total revenue" value={loading ? "…" : currency.format(revenue)} detail="Confirmed order revenue" Icon={BarChart3} tone="bg-emerald-50 text-emerald-700"/><MetricCard label="Total orders" value={loading ? "…" : String(dashboard?.totalOrders ?? 0)} detail="Across every order status" Icon={ShoppingBag} tone="bg-orange-50 text-app-orange"/><MetricCard label="Active employees" value={loading ? "…" : String(dashboard?.activeEmployees ?? 0)} detail="Team members with access" Icon={Users} tone="bg-amber-50 text-amber-700"/><MetricCard label="Low stock items" value={loading ? "…" : String(dashboard?.lowStockProducts ?? 0)} detail="10 units or fewer remaining" Icon={PackageSearch} tone="bg-rose-50 text-rose-600"/><MetricCard label="Pending requests" value={loading ? "…" : String(dashboard?.pendingRequests ?? 0)} detail="Awaiting an admin decision" Icon={ClipboardCheck} tone="bg-sky-50 text-sky-700"/></div><div className="grid gap-6 xl:grid-cols-[minmax(0,1.8fr)_minmax(310px,.9fr)]"><SalesAnalytics range={range} onRange={setRange} reports={reports} previousRevenue={previousRevenue}/><OrderStatus statuses={dashboard?.orderStatuses ?? {}} total={dashboard?.totalOrders ?? 0}/></div><div className="grid gap-6 xl:grid-cols-[minmax(0,1.65fr)_minmax(310px,.8fr)]"><RecentOrders orders={dashboard?.recentOrders ?? []}/><LowStock products={dashboard?.lowStockItems ?? []}/></div><Highlights/></section>;
}
