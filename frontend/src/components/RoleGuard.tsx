import { Navigate, Outlet } from "react-router-dom";
import type { Role } from "../types";
import { useAuth } from "../context/AuthContext";

export default function RoleGuard({ allow }: { allow: Role[] }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="min-h-screen flex-center">Loading Grocers...</div>;
  if (!user) return <Navigate to="/auth" replace />;
  const adminAllowed = (user.role === "ADMIN" || user.role === "SUPER_ADMIN") && allow.includes("ADMIN");
  if (!allow.includes(user.role) && !adminAllowed) return <Navigate to={(user.role === "ADMIN" || user.role === "SUPER_ADMIN") ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/"} replace />;
  if (user.role === "EMPLOYEE" && user.mustChangePassword && !location.pathname.includes("/employee/profile")) return <Navigate to="/employee/profile" replace />;
  return <Outlet />;
}
