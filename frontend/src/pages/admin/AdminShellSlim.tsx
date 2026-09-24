import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  BarChart3, Bell, ChevronDown, ChevronLeft, LayoutDashboard, LogOut, Menu,
  Package, PackageSearch, Settings2, ShieldCheck, ShoppingBag, UserCircle, Users, X,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import api from "../../config/api";

const navigation = [
  { to: "/admin", label: "Overview", icon: LayoutDashboard },
  { to: "/admin/orders", label: "Orders", icon: ShoppingBag },
  { to: "/admin/products", label: "Products", icon: Package },
  { to: "/admin/requests", label: "Requests", icon: PackageSearch },
  { to: "/admin/users", label: "Customers", icon: Users },
  { to: "/admin/employees", label: "Employees", icon: ShieldCheck },
  { to: "/admin/reports", label: "Reports", icon: BarChart3 },
  { to: "/admin/admins", label: "Admin accounts", icon: Settings2, superAdmin: true },
  { to: "/admin/profile", label: "My profile", icon: UserCircle },
] as const;
type AdminNotification = {
  id: number;
  requestId: number;
  employeeId: number;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
};

export default function AdminShellSlim() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user } = useAuth();

  const links = navigation.filter((item) => !("superAdmin" in item) || user?.role === "SUPER_ADMIN");
  const activePage = links.find((item) => item.to === location.pathname)?.label ?? "Admin workspace";
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [notificationOpen, setNotificationOpen] = useState(false);
  useEffect(() => {
    setMobileOpen(false); setProfileOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (location.pathname !== "/admin") sessionStorage.setItem("grocers_last_admin_page", location.pathname + location.search + location.hash);
  }, [location.pathname, location.search, location.hash]);

  useEffect(() => {
    const navigation = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
    const lastPage = sessionStorage.getItem("grocers_last_admin_page");
    if (navigation?.type === "reload" && location.pathname === "/admin" && lastPage?.startsWith("/admin/")) navigate(lastPage, { replace: true });
  }, [location.pathname, navigate]);

  const go = (to: string) => { navigate(to); };
  useEffect(() => {
    const loadNotifications = async () => {
      try {
        const { data } = await api.get<AdminNotification[]>(
            "/admin/notifications",
        );

        setNotifications(Array.isArray(data) ? data : []);
      } catch {
        // Notifications are optional UI data; do not break the admin layout.
      }
    };

    void loadNotifications();

    const timer = window.setInterval(() => {
      void loadNotifications();
    }, 10000);

    return () => window.clearInterval(timer);
  }, []);
  const unreadCount = notifications.filter(
      (notification) => !notification.read,
  ).length;

  const openNotification = async (notification: AdminNotification) => {
    setNotificationOpen(false);

    if (!notification.read) {
      try {
        await api.patch(`/admin/notifications/${notification.id}/read`);

        setNotifications((current) =>
            current.map((item) =>
                item.id === notification.id
                    ? { ...item, read: true }
                    : item,
            ),
        );
      } catch {
        // The request page can still be opened if marking read fails.
      }
    }

    navigate("/admin/requests");
  };
  return (
    <div className="min-h-screen bg-[#f5f7f4] text-[#17231b]">
      {mobileOpen && <button onClick={() => setMobileOpen(false)} className="fixed inset-0 z-30 bg-[#101b14]/40 backdrop-blur-[2px] lg:hidden" aria-label="Close navigation" />}

      <aside className={`fixed inset-y-0 left-0 z-40 flex flex-col border-r border-white/10 bg-[#13251a] text-white shadow-2xl transition-[width,transform] duration-300 ease-out ${collapsed ? "w-[76px]" : "w-[248px]"} ${mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}`}>
        <div className={`flex h-[76px] items-center gap-3 border-b border-white/10 px-4 ${collapsed ? "justify-center" : ""}`}>
          {!collapsed && <button onClick={() => go("/admin")} aria-label="Grocers overview" className="grid size-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-[#ffb45c] to-[#f27621] shadow-lg shadow-orange-950/20"><img src="/main_logo.png" alt="" className="size-7 object-contain" /></button>}
          {!collapsed && <div className="min-w-0 flex-1"><p className="truncate text-lg font-bold tracking-tight">GROCERS</p><p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-white/40">Operations</p></div>}
          <button onClick={() => setCollapsed((value) => !value)} className="hidden size-8 place-items-center rounded-lg text-white/50 hover:bg-white/10 hover:text-white lg:grid" aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}><ChevronLeft className={collapsed ? "rotate-180" : ""} size={17} /></button>
          <button onClick={() => setMobileOpen(false)} className="grid size-8 place-items-center rounded-lg text-white/60 hover:bg-white/10 lg:hidden" aria-label="Close sidebar"><X size={18} /></button>
        </div>

        <nav className="flex flex-1 flex-col overflow-y-auto px-3 py-5">
          {!collapsed && <p className="mb-2 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-white/30">Workspace</p>}
          <div className="space-y-1">
            {links.map(({ to, label, icon: Icon }) => (
              <NavLink end={to === "/admin"} key={to} to={to} title={collapsed ? label : undefined} className={({ isActive }) => `group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${isActive ? "bg-white text-[#13251a] shadow-lg shadow-black/10" : "text-white/58 hover:bg-white/[0.07] hover:text-white"}`}>
                {({ isActive }) => <><Icon size={18} strokeWidth={isActive ? 2.4 : 1.9} className="shrink-0" />{!collapsed && <span className="truncate">{label}</span>}{isActive && !collapsed && <span className="ml-auto size-1.5 rounded-full bg-[#f27621]" />}</>}
              </NavLink>
            ))}
          </div>
          {collapsed && <button onClick={() => go("/admin")} title="Grocers overview" className="mt-auto grid size-10 self-center place-items-center rounded-xl bg-gradient-to-br from-[#ffb45c] to-[#f27621] shadow-lg shadow-black/20"><img src="/main_logo.png" alt="" className="size-7 object-contain" /></button>}
        </nav>

        <div className="border-t border-white/10 p-3">
          <button onClick={() => go("/admin/profile")} className={`flex w-full items-center gap-3 rounded-xl p-2 text-left hover:bg-white/[0.07] ${collapsed ? "justify-center" : ""}`}>
            <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-white/10 text-sm font-bold text-orange-200">{(user?.firstName?.[0] || user?.name?.[0] || "A").toUpperCase()}</span>
            {!collapsed && <span className="min-w-0 flex-1"><b className="block truncate text-sm">{user?.name || "Administrator"}</b><span className="block truncate text-[11px] text-white/40">{user?.email || "Store operations"}</span></span>}
          </button>
        </div>
      </aside>

      <main className={`min-h-screen transition-[margin] duration-300 ease-out ${collapsed ? "lg:ml-[76px]" : "lg:ml-[248px]"}`}>
        <header className="sticky top-0 z-20 flex h-[76px] items-center gap-3 border-b border-[#dfe5df] bg-[#f5f7f4]/90 px-4 backdrop-blur-xl sm:px-6 xl:px-8">
          <button onClick={() => setMobileOpen(true)} className="grid size-10 place-items-center rounded-xl border border-[#dfe5df] bg-white text-[#34483a] lg:hidden" aria-label="Open navigation"><Menu size={19} /></button>
          <div className="min-w-0"><p className="truncate text-[11px] font-bold uppercase tracking-[0.16em] text-[#8a978d]">Admin / {activePage}</p><h1 className="truncate text-lg font-bold tracking-tight sm:text-xl">{activePage}</h1></div>

          <div className="relative ml-auto">
            <button
                onClick={() => setNotificationOpen((value) => !value)}
                className="relative grid size-10 place-items-center rounded-xl border border-[#dfe5df] bg-white text-[#536258] shadow-sm transition hover:-translate-y-0.5"
                aria-label="Open notifications"
            >
              <Bell size={18} />

              {unreadCount > 0 && (
                  <span className="absolute -right-1 -top-1 grid min-w-5 h-5 place-items-center rounded-full border-2 border-white bg-[#f27621] px-1 text-[10px] font-bold text-white">
        {unreadCount > 9 ? "9+" : unreadCount}
      </span>
              )}
            </button>

            {notificationOpen && (
                <div className="absolute right-0 top-12 z-50 w-80 overflow-hidden rounded-2xl border border-[#dfe5df] bg-white shadow-xl">
                  <div className="flex items-center justify-between border-b border-[#edf0ed] px-4 py-3">
                    <div>
                      <h3 className="font-semibold text-[#284331]">Notifications</h3>
                      <p className="text-xs text-[#829087]">
                        {unreadCount} unread request{unreadCount === 1 ? "" : "s"}
                      </p>
                    </div>

                    <button
                        onClick={() => {
                          setNotificationOpen(false);
                          go("/admin/requests");
                        }}
                        className="text-xs font-semibold text-app-orange hover:underline"
                    >
                      View all
                    </button>
                  </div>

                  <div className="max-h-96 overflow-y-auto">
                    {notifications.length === 0 ? (
                        <p className="p-5 text-center text-sm text-[#829087]">
                          No notifications yet.
                        </p>
                    ) : (
                        notifications.slice(0, 6).map((notification) => (
                            <button
                                key={notification.id}
                                onClick={() => void openNotification(notification)}
                                className={`w-full border-b border-[#edf0ed] px-4 py-3 text-left transition hover:bg-[#f7faf7] ${
                                    notification.read ? "bg-white" : "bg-orange-50/60"
                                }`}
                            >
                              <div className="flex items-start gap-3">
                                {!notification.read && (
                                    <span className="mt-1.5 size-2 shrink-0 rounded-full bg-app-orange" />
                                )}

                                <div className={notification.read ? "ml-5" : ""}>
                                  <p className="text-sm font-semibold text-[#284331]">
                                    {notification.title}
                                  </p>

                                  <p className="mt-1 text-xs leading-5 text-[#657269]">
                                    {notification.message}
                                  </p>

                                  <p className="mt-1 text-[11px] text-[#98a39b]">
                                    Request #{notification.requestId}
                                  </p>
                                </div>
                              </div>
                            </button>
                        ))
                    )}
                  </div>
                </div>
            )}
          </div>

          <div className="relative">
            <button onClick={() => setProfileOpen((value) => !value)} className="flex h-10 items-center gap-2 rounded-xl border border-[#dfe5df] bg-white px-1.5 pr-2.5 text-left shadow-sm"><span className="grid size-7 place-items-center rounded-lg bg-[#e8efe9] text-xs font-bold text-[#284331]">{(user?.firstName?.[0] || "A").toUpperCase()}</span><span className="hidden max-w-28 truncate text-sm font-semibold sm:block">{user?.firstName || "Admin"}</span><ChevronDown size={14} className="text-[#819087]" /></button>
            {profileOpen && <div className="absolute right-0 top-12 w-56 rounded-xl border border-[#dfe5df] bg-white p-1.5 shadow-xl"><div className="border-b border-[#edf0ed] px-3 py-2.5"><b className="block truncate text-sm">{user?.name || "Administrator"}</b><span className="block truncate text-xs text-[#829087]">{user?.email}</span></div><button onClick={() => go("/admin/profile")} className="mt-1 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-[#f2f6f2]"><UserCircle size={16} />My profile</button><button onClick={logout} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-rose-600 hover:bg-rose-50"><LogOut size={16} />Sign out</button></div>}
          </div>
        </header>

        <div className="mx-auto max-w-[1580px] p-4 sm:p-6 xl:p-8"><Outlet /></div>
      </main>
    </div>
  );
}
