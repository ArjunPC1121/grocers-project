import { Navigate, Outlet, useLocation } from "react-router-dom";
import type { Role } from "../types";
import { useAuth } from "../context/AuthContext";

export default function RoleGuard({ allow }: { allow: Role[] }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  // Wait for saved session data before deciding whether this URL is allowed.
  if (loading) return <div className="min-h-screen flex-center">Loading Grocers...</div>;

  if (!user) {
    return <Navigate to="/auth" replace state={{ returnTo: location.pathname + location.search + location.hash }} />;
  }

  // Super admins share the normal admin workspace permissions.
  const adminAllowed = (user.role === "ADMIN" || user.role === "SUPER_ADMIN") && allow.includes("ADMIN");
  if (!allow.includes(user.role) && !adminAllowed) {
    const destination = user.role === "ADMIN" || user.role === "SUPER_ADMIN" ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/";
    return <Navigate to={destination} replace />;
  }

  // New employee passwords must be changed before the employee can use the workspace.
  if (user.role === "EMPLOYEE" && user.mustChangePassword && !location.pathname.includes("/employee/change-password")) {
    return <Navigate to="/employee/change-password" replace />;
  }

  return <Outlet />;
}
