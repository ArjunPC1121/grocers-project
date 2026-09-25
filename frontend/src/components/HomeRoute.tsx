import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Home from "../pages/Home";

export default function HomeRoute() {
  const { user, loading } = useAuth();
  if (loading) return <div className="min-h-screen flex-center">Loading Grocers...</div>;
  if (user?.role === "EMPLOYEE") return <Navigate to="/employee" replace />;
  return <Home />;
}
