import { Navigate, Outlet, useLocation } from "react-router-dom";
import type { Role } from "../types";
import { useAuth } from "../context/AuthContext";

export default function RoleGuard({ allow }: { allow: Role[] }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="min-h-screen flex-center">Loading Grocers...</div>;
  if (!user) return <Navigate to="/auth" replace state={{ returnTo: location.pathname + location.search + location.hash }} />;
  if (!allow.includes(user.role)) return <Navigate to={user.role === "ADMIN" ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/"} replace />;
  if (user.role === "EMPLOYEE" && user.mustChangePassword && !location.pathname.includes("/employee/profile")) return <Navigate to="/employee/profile" replace />;
  return <Outlet />;
}
