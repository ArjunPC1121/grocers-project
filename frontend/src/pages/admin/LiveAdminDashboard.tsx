import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowDownRight, ArrowRight, ArrowUpRight, Boxes, CheckCircle2, ChevronRight,
  CirclePlus, ClipboardCheck, IndianRupee, Package, PackageSearch, RefreshCw,
  ShoppingBag, TrendingUp, Users,
} from "lucide-react";
import api, { errorMessage } from "../../config/api";
import { useAuth } from "../../context/AuthContext";

type Row = Record<string, unknown>;
type Range = "week" | "month" | "quarter";
type Dashboard = {
  totalProducts: number;
  totalUsers: number;
  inventoryUnits: number;
  inventoryValue: number | string;
  lowStockProducts: number;
  activeEmployees: number;
  pendingRequests: number;
  totalOrders: number;
  revenue: number | string;
  averageOrderValue: number | string;
  fulfilledOrders: number;
  fulfilmentRate: number;
  orderStatuses: Record<string, number>;
  requestStatuses: Record<string, number>;
  categoryInventory: Record<string, number>;
  recentOrders: Row[];
  recentRequests: Row[];
  lowStockItems: Row[];
};
type Report = { revenue: number | string; orders: Row[] };
type Point = { key: string; label: string; value: number };

const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const compact = new Intl.NumberFormat("en-IN", { notation: "compact", maximumFractionDigits: 1 });
const today = () => new Date().toISOString().slice(0, 10);
const text = (value: unknown) => String(value ?? "");
const number = (value: unknown) => Number(value ?? 0) || 0;
const title = (value: string) => value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
const orderDate = (value: unknown) => {
  const date = new Date(text(value));
  return Number.isNaN(date.getTime()) ? "Recently" : new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" }).format(date);
};

function shiftMonth(offset: number) {
  const date = new Date(`${today()}T12:00:00`); date.setMonth(date.getMonth() + offset); return date.toISOString().slice(0, 10);
}
function shiftDays(offset: number) {
  const date = new Date(`${today()}T12:00:00`); date.setDate(date.getDate() + offset); return date.toISOString().slice(0, 10);
}
function reportRequest(range: Range, offset = 0) {
  if (range === "week") return api.get<Report>("/admin/reports", { params: { period: "WEEKLY", referenceDate: shiftDays(offset * 7) } });
  return api.get<Report>("/admin/reports", { params: { period: "MONTHLY", referenceDate: shiftMonth(offset) } });
}

function seriesFor(range: Range, reports: Report[]): Point[] {
  if (range === "quarter") {
    return reports.slice().reverse().map((report, index) => ({
      key: String(index),
      label: new Intl.DateTimeFormat("en", { month: "short" }).format(new Date(`${shiftMonth(index - 2)}T12:00:00`)),
      value: number(report.revenue),
    }));
  }
  const current = new Date(`${today()}T12:00:00`);
  const start = new Date(current);
  if (range === "week") start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  else start.setDate(1);
  const length = range === "week" ? 7 : new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate();
  const buckets = Array.from({ length }, (_, index) => {
    const date = new Date(start); date.setDate(date.getDate() + index);
    return { key: date.toISOString().slice(0, 10), label: range === "week" ? new Intl.DateTimeFormat("en", { weekday: "short" }).format(date) : String(index + 1), value: 0 };
  });
  reports[0]?.orders.forEach((order) => {
    const bucket = buckets.find((item) => item.key === text(order.orderedAt).slice(0, 10));
    if (bucket) bucket.value += number(order.totalAmount);
  });
  return buckets;
}

function MetricCard({ label, value, note, icon: Icon, accent }: { label: string; value: string; note: string; icon: typeof ShoppingBag; accent: string }) {
  return (
    <article className="group rounded-2xl border border-[#dfe5df] bg-white p-5 shadow-[0_1px_2px_rgba(20,40,25,.03)] transition duration-300 hover:-translate-y-0.5 hover:shadow-[0_12px_28px_rgba(20,40,25,.07)]">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0"><p className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#829087]">{label}</p><p className="mt-3 truncate text-2xl font-bold tracking-tight text-[#17231b] sm:text-[28px]">{value}</p></div>
        <span className={`grid size-10 shrink-0 place-items-center rounded-xl ${accent}`}><Icon size={19} /></span>
      </div>
      <p className="mt-2 truncate text-xs text-[#77867c]">{note}</p>
    </article>
  );
}

function TrendBadge({ value }: { value: number | null }) {
  if (value === null) return <span className="rounded-full bg-[#f1f4f1] px-2.5 py-1 text-xs font-semibold text-[#77867c]">No prior data</span>;
  const positive = value >= 0;
  const Icon = positive ? ArrowUpRight : ArrowDownRight;
  return <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-bold ${positive ? "bg-emerald-50 text-emerald-700" : "bg-rose-50 text-rose-700"}`}><Icon size={13} />{Math.abs(value).toFixed(1)}%</span>;
}

function RevenueChart({ range, onRange, reports, previousRevenue }: { range: Range; onRange: (range: Range) => void; reports: Report[]; previousRevenue: number }) {
  const [hovered, setHovered] = useState<number | null>(null);
  const points = useMemo(() => seriesFor(range, reports), [range, reports]);
  const revenue = reports.reduce((sum, report) => sum + number(report.revenue), 0);
  const change = previousRevenue ? ((revenue - previousRevenue) / previousRevenue) * 100 : null;
  const max = Math.max(...points.map((point) => point.value), 1);
  const width = 760, height = 245, left = 30, top = 20, bottom = 38, chartHeight = height - top - bottom, chartWidth = width - left - 14;
  const coords = points.map((point, index) => ({ ...point, x: left + (points.length === 1 ? chartWidth / 2 : (index / (points.length - 1)) * chartWidth), y: top + chartHeight - (point.value / max) * chartHeight }));
  const line = coords.map((point) => `${point.x},${point.y}`).join(" ");
  const area = coords.length ? `${left},${top + chartHeight} ${line} ${left + chartWidth},${top + chartHeight}` : "";
  const shownLabels = range === "month" ? coords.filter((_, index) => index === 0 || (index + 1) % 5 === 0 || index === coords.length - 1) : coords;

  return (
    <article className="rounded-2xl border border-[#dfe5df] bg-white p-5 shadow-[0_1px_2px_rgba(20,40,25,.03)] sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#829087]">Performance</p><h3 className="mt-1 text-lg font-bold">Revenue overview</h3><div className="mt-3 flex items-center gap-3"><b className="text-2xl tracking-tight">{currency.format(revenue)}</b><TrendBadge value={change} /></div></div>
        <div className="flex rounded-xl bg-[#f1f4f1] p-1">{(["week", "month", "quarter"] as Range[]).map((item) => <button key={item} onClick={() => onRange(item)} className={`rounded-lg px-3 py-1.5 text-xs font-bold capitalize ${range === item ? "bg-white text-[#1d3a28] shadow-sm" : "text-[#7d8b81] hover:text-[#1d3a28]"}`}>{item}</button>)}</div>
      </div>
      <div className="mt-5 overflow-hidden">
        <svg viewBox={`0 0 ${width} ${height}`} className="h-[245px] w-full overflow-visible" role="img" aria-label="Revenue trend chart" onMouseLeave={() => setHovered(null)}>
          <defs><linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#2f7650" stopOpacity="0.24" /><stop offset="100%" stopColor="#2f7650" stopOpacity="0" /></linearGradient></defs>
          {[0, .33, .66, 1].map((step) => <line key={step} x1={left} x2={left + chartWidth} y1={top + chartHeight * step} y2={top + chartHeight * step} stroke="#e7ece8" strokeDasharray="4 6" />)}
          {area && <polygon points={area} fill="url(#revenueFill)" />}
          {line && <polyline points={line} fill="none" stroke="#285d3d" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />}
          {coords.map((point, index) => <g key={point.key} onMouseEnter={() => setHovered(index)} className="cursor-pointer"><circle cx={point.x} cy={point.y} r="11" fill="transparent" /><circle cx={point.x} cy={point.y} r={hovered === index ? 5 : 3.5} fill="#fff" stroke="#285d3d" strokeWidth="2.5" />{hovered === index && <><line x1={point.x} x2={point.x} y1={point.y + 8} y2={top + chartHeight} stroke="#9aaba0" strokeDasharray="3 4" /><g transform={`translate(${Math.max(54, Math.min(width - 55, point.x)) - 54},${Math.max(3, point.y - 45)})`}><rect width="108" height="34" rx="8" fill="#13251a" /><text x="54" y="14" textAnchor="middle" fill="#b9c7bc" fontSize="9">{point.label}</text><text x="54" y="27" textAnchor="middle" fill="white" fontSize="11" fontWeight="700">{currency.format(point.value)}</text></g></>}</g>)}
          {shownLabels.map((point) => <text key={`label-${point.key}`} x={point.x} y={height - 10} textAnchor="middle" fill="#87948b" fontSize="10">{point.label}</text>)}
        </svg>
      </div>
    </article>
  );
}

function StatusCard({ statuses, total }: { statuses: Record<string, number>; total: number }) {
  const colors = ["#2f7650", "#f28a3a", "#e9be52", "#7c73c7", "#dd6b77", "#69a7c9"];
  const entries = Object.entries(statuses).sort((left, right) => right[1] - left[1]);
  const stops = entries.reduce<{ cursor: number; values: string[] }>((result, [, count], index) => {
    const end = result.cursor + (total ? (count / total) * 100 : 0);
    return { cursor: end, values: [...result.values, `${colors[index % colors.length]} ${result.cursor}% ${end}%`] };
  }, { cursor: 0, values: [] }).values.join(", ");
  return (
    <article className="rounded-2xl border border-[#dfe5df] bg-white p-5 shadow-[0_1px_2px_rgba(20,40,25,.03)] sm:p-6">
      <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#829087]">Operations</p><h3 className="mt-1 text-lg font-bold">Order status</h3>
      <div className="mt-5 flex items-center gap-6">
        <div className="relative grid size-32 shrink-0 place-items-center rounded-full" style={{ background: stops ? `conic-gradient(${stops})` : "#eef2ee" }}><div className="grid size-[82px] place-items-center rounded-full bg-white text-center shadow-inner"><span><b className="block text-2xl">{total}</b><small className="text-[10px] font-semibold uppercase tracking-wide text-[#89968d]">Orders</small></span></div></div>
        <div className="min-w-0 flex-1 space-y-3">{entries.slice(0, 5).map(([status, count], index) => <div key={status} className="flex items-center gap-2 text-sm"><i className="size-2 rounded-full" style={{ background: colors[index % colors.length] }} /><span className="min-w-0 flex-1 truncate text-[#647269]">{title(status)}</span><b>{count}</b></div>)}{!entries.length && <p className="text-sm text-[#77867c]">No orders yet.</p>}</div>
      </div>
    </article>
  );
}

function StatusPill({ status }: { status: string }) {
  const styles: Record<string, string> = { DELIVERED: "bg-emerald-50 text-emerald-700", OUT_FOR_DELIVERY: "bg-amber-50 text-amber-700", SHIPPED: "bg-violet-50 text-violet-700", PLACED: "bg-sky-50 text-sky-700", CREATED: "bg-zinc-100 text-zinc-600", CANCELLED: "bg-rose-50 text-rose-700", PENDING: "bg-amber-50 text-amber-700", APPROVED: "bg-emerald-50 text-emerald-700", REJECTED: "bg-rose-50 text-rose-700" };
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide ${styles[status] || "bg-zinc-100 text-zinc-600"}`}>{title(status || "Unknown")}</span>;
}

function RecentOrders({ orders }: { orders: Row[] }) {
  const navigate = useNavigate();
  return (
    <article className="overflow-hidden rounded-2xl border border-[#dfe5df] bg-white shadow-[0_1px_2px_rgba(20,40,25,.03)]">
      <div className="flex items-center justify-between gap-4 p-5 sm:p-6"><div><p className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#829087]">Latest activity</p><h3 className="mt-1 text-lg font-bold">Recent orders</h3></div><button onClick={() => navigate("/admin/reports")} className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold text-[#2d6846] hover:bg-emerald-50">View report <ChevronRight size={14} /></button></div>
      <div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="border-y border-[#e8ece8] bg-[#f8faf8] text-[10px] font-bold uppercase tracking-[0.12em] text-[#87948b]"><tr>{["Order", "Customer", "Payment", "Status", "Amount"].map((label) => <th key={label} className="px-5 py-3">{label}</th>)}</tr></thead><tbody>{orders.map((order) => <tr key={text(order.id) || text(order.orderNumber)} className="border-b border-[#edf0ed] transition hover:bg-[#fafcf9]"><td className="px-5 py-4"><b className="block text-[#294c36]">{text(order.orderNumber) || `#${text(order.id)}`}</b><span className="mt-0.5 block text-xs text-[#8a978d]">{orderDate(order.orderedAt)}</span></td><td className="px-5 py-4"><b className="block max-w-40 truncate font-semibold">{text(order.customerName) || `Customer #${text(order.customerId)}`}</b><span className="block max-w-44 truncate text-xs text-[#8a978d]">{text(order.customerEmail) || `${Array.isArray(order.items) ? order.items.length : 0} items`}</span></td><td className="px-5 py-4 text-[#657269]">{title(text(order.paymentMethod) || "Not recorded")}</td><td className="px-5 py-4"><StatusPill status={text(order.status)} /></td><td className="px-5 py-4 font-bold">{currency.format(number(order.totalAmount))}</td></tr>)}{!orders.length && <tr><td colSpan={5} className="px-5 py-12 text-center text-[#7b897f]">No orders have been placed yet.</td></tr>}</tbody></table></div>
    </article>
  );
}

function AttentionPanel({ dashboard }: { dashboard: Dashboard | null }) {
  const navigate = useNavigate();
  const requests = dashboard?.recentRequests ?? [];
  const stock = dashboard?.lowStockItems ?? [];
  return (
    <article className="rounded-2xl border border-[#dfe5df] bg-white p-5 shadow-[0_1px_2px_rgba(20,40,25,.03)] sm:p-6">
      <div className="flex items-center justify-between"><div><p className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#829087]">Needs attention</p><h3 className="mt-1 text-lg font-bold">Action centre</h3></div><span className="grid size-9 place-items-center rounded-xl bg-orange-50 text-[#d96b1d]"><ClipboardCheck size={18} /></span></div>
      <div className="mt-5 space-y-5">
        <section><div className="mb-3 flex items-center justify-between"><b className="text-sm">Inventory requests</b><button onClick={() => navigate("/admin/requests")} className="text-xs font-bold text-[#2d6846]">Review all</button></div><div className="space-y-2">{requests.slice(0, 2).map((request) => <button key={text(request.requestId) || text(request.id)} onClick={() => navigate("/admin/requests")} className="flex w-full items-center gap-3 rounded-xl bg-[#f7f9f7] p-3 text-left hover:bg-[#eef4ef]"><span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white text-[#2f7650] shadow-sm"><PackageSearch size={17} /></span><span className="min-w-0 flex-1"><b className="block truncate text-sm">{title(text(request.action) || "Product request")}</b><small className="block truncate text-[#829087]">Employee #{text(request.employeeId) || "—"}</small></span><StatusPill status={text(request.status)} /></button>)}{!requests.length && <p className="rounded-xl bg-[#f7f9f7] p-3 text-sm text-[#77867c]">No product requests need review.</p>}</div></section>
        <section className="border-t border-[#e8ece8] pt-5"><div className="mb-3 flex items-center justify-between"><b className="text-sm">Low stock</b><button onClick={() => navigate("/admin/products")} className="text-xs font-bold text-[#2d6846]">Manage stock</button></div><div className="space-y-3">{stock.slice(0, 3).map((product) => <div key={text(product.id)} className="flex items-center gap-3"><span className="grid size-9 shrink-0 place-items-center overflow-hidden rounded-lg bg-[#f2f5f1]">{text(product.imageUrl) ? <img src={text(product.imageUrl)} alt="" className="size-full object-cover" /> : <Package size={16} className="text-[#7e8d82]" />}</span><span className="min-w-0 flex-1"><b className="block truncate text-sm">{text(product.name) || "Unnamed product"}</b><small className="block truncate text-[#829087]">{text(product.category) || "Uncategorised"}</small></span><b className={`text-sm ${number(product.quantity) === 0 ? "text-rose-600" : "text-amber-700"}`}>{number(product.quantity)} left</b></div>)}{!stock.length && <p className="flex items-center gap-2 rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700"><CheckCircle2 size={16} />Stock levels look healthy.</p>}</div></section>
      </div>
    </article>
  );
}

export default function LiveAdminDashboard() {
  const [range, setRange] = useState<Range>("week");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [previousRevenue, setPreviousRevenue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const navigate = useNavigate();
  const { user } = useAuth();

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true); setError("");
    try {
      const currentOffsets = range === "quarter" ? [0, -1, -2] : [0];
      const previousOffsets = range === "quarter" ? [-3, -4, -5] : [-1];
      const [dashboardResponse, current, previous] = await Promise.all([
        api.get<Dashboard>("/admin/dashboard"),
        Promise.all(currentOffsets.map((offset) => reportRequest(range, offset))),
        Promise.all(previousOffsets.map((offset) => reportRequest(range, offset))),
      ]);
      setDashboard(dashboardResponse.data);
      setReports(current.map((response) => ({ ...response.data, orders: Array.isArray(response.data.orders) ? response.data.orders : [] })));
      setPreviousRevenue(previous.reduce((sum, response) => sum + number(response.data.revenue), 0));
      setUpdatedAt(new Date());
    } catch (requestError) { setError(errorMessage(requestError, "Live dashboard data is unavailable.")); }
    finally { setLoading(false); }
  }, [range]);

  useEffect(() => {
    load(); const interval = window.setInterval(() => load(true), 60000); return () => window.clearInterval(interval);
  }, [load]);

  const greeting = new Date().getHours() < 12 ? "Good morning" : new Date().getHours() < 18 ? "Good afternoon" : "Good evening";
  const date = new Intl.DateTimeFormat("en-IN", { weekday: "long", day: "numeric", month: "long" }).format(new Date());
  const categoryCount = Object.keys(dashboard?.categoryInventory ?? {}).length;

  return (
    <section className="animate-fade-in space-y-6 pb-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div><p className="text-sm font-medium text-[#718077]">{date}</p><h2 className="mt-1 text-3xl font-bold tracking-tight sm:text-[34px]">{greeting}, {user?.firstName || "Admin"}</h2><p className="mt-1 text-sm text-[#718077]">Here’s what is happening across your store.</p></div>
        <div className="flex items-center gap-2"><button onClick={() => load()} disabled={loading} className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#d7ded8] bg-white px-3.5 text-sm font-semibold text-[#435248] shadow-sm hover:-translate-y-0.5 disabled:opacity-60"><RefreshCw className={loading ? "animate-spin" : ""} size={15} />Refresh</button><button onClick={() => navigate("/admin/products")} className="inline-flex h-10 items-center gap-2 rounded-xl bg-[#204f34] px-4 text-sm font-bold text-white shadow-lg shadow-emerald-950/10 hover:-translate-y-0.5 hover:bg-[#173e28]"><CirclePlus size={16} />Add product</button></div>
      </div>

      {error && <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"><span><b>Dashboard refresh failed.</b> {error}</span><button onClick={() => load()} className="font-bold underline">Try again</button></div>}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Total revenue" value={loading && !dashboard ? "—" : currency.format(number(dashboard?.revenue))} note={`${dashboard?.totalOrders ?? 0} orders across all statuses`} icon={IndianRupee} accent="bg-emerald-50 text-emerald-700" />
        <MetricCard label="Average order" value={loading && !dashboard ? "—" : currency.format(number(dashboard?.averageOrderValue))} note="Average confirmed basket value" icon={TrendingUp} accent="bg-orange-50 text-orange-700" />
        <MetricCard label="Customers" value={loading && !dashboard ? "—" : compact.format(dashboard?.totalUsers ?? 0)} note="Registered customer accounts" icon={Users} accent="bg-sky-50 text-sky-700" />
        <MetricCard label="Inventory value" value={loading && !dashboard ? "—" : currency.format(number(dashboard?.inventoryValue))} note={`${compact.format(dashboard?.inventoryUnits ?? 0)} units in ${categoryCount} categories`} icon={Boxes} accent="bg-violet-50 text-violet-700" />
      </div>

      <div className="grid overflow-hidden rounded-2xl border border-[#dfe5df] bg-white sm:grid-cols-2 xl:grid-cols-4">
        {[{ label: "Active employees", value: dashboard?.activeEmployees ?? 0, detail: "Available staff", icon: Users, tone: "text-sky-700 bg-sky-50" }, { label: "Pending requests", value: dashboard?.pendingRequests ?? 0, detail: "Awaiting review", icon: ClipboardCheck, tone: "text-amber-700 bg-amber-50" }, { label: "Low stock", value: dashboard?.lowStockProducts ?? 0, detail: "10 units or fewer", icon: PackageSearch, tone: "text-rose-700 bg-rose-50" }, { label: "Fulfilment rate", value: `${dashboard?.fulfilmentRate ?? 0}%`, detail: `${dashboard?.fulfilledOrders ?? 0} orders delivered`, icon: CheckCircle2, tone: "text-emerald-700 bg-emerald-50" }].map(({ label, value, detail, icon: Icon, tone }, index) => <div key={label} className={`flex items-center gap-3 p-4 sm:p-5 ${index ? "border-t border-[#e8ece8] sm:border-l sm:border-t-0" : ""} ${index === 2 ? "sm:border-l-0 xl:border-l" : ""}`}><span className={`grid size-9 shrink-0 place-items-center rounded-xl ${tone}`}><Icon size={17} /></span><div className="min-w-0"><b className="block text-xl">{value}</b><p className="truncate text-xs text-[#7b897f]">{label} · {detail}</p></div></div>)}
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.7fr)_minmax(315px,.72fr)]"><RevenueChart range={range} onRange={setRange} reports={reports} previousRevenue={previousRevenue} /><StatusCard statuses={dashboard?.orderStatuses ?? {}} total={dashboard?.totalOrders ?? 0} /></div>
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(330px,.72fr)]"><RecentOrders orders={dashboard?.recentOrders ?? []} /><AttentionPanel dashboard={dashboard} /></div>

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-[#13251a] px-5 py-4 text-white sm:px-6"><div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-white/10 text-orange-300"><ShoppingBag size={17} /></span><div><b className="text-sm">Need a deeper view?</b><p className="text-xs text-white/50">Filter revenue by date, product, or customer in Reports.</p></div></div><button onClick={() => navigate("/admin/reports")} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-xs font-bold text-[#173522]">Open reports <ArrowRight size={14} /></button></div>
      {updatedAt && <p className="text-right text-[11px] text-[#91a096]">Live data from products, orders, users, employees and requests · Updated {updatedAt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</p>}
    </section>
  );
}
