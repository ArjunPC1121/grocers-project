import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import toast from "react-hot-toast";
import api, { errorMessage } from "../config/api";
import type { Role, SessionUser } from "../types";
type AuthContextType = { user: SessionUser | null; loading: boolean; login: (role: Role, identifier: string, password: string) => Promise<void>; register: (payload: Record<string, string>) => Promise<void>; logout: () => void; updateUser: (value: Partial<SessionUser>) => void; };
const AuthContext = createContext<AuthContextType | undefined>(undefined);
const destination = (user: SessionUser) => user.role === "EMPLOYEE" && user.mustChangePassword ? "/employee/change-password" : user.role === "ADMIN" ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/";
const roleFromApi = (role: string): Role => role === "USER" ? "CUSTOMER" : role as Role;
const tokenSubject = (token?: string) => {
  if (!token) return "";
  try { return JSON.parse(atob(token.split(".")[1])).sub || ""; } catch { return ""; }
};
export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate(); const [user, setUser] = useState<SessionUser | null>(null); const [loading, setLoading] = useState(true);
  useEffect(() => { try { const saved = localStorage.getItem("grocers_session"); if (saved) setUser(JSON.parse(saved)); } catch { localStorage.removeItem("grocers_session"); } finally { setLoading(false); } }, []);
  const save = (next: SessionUser, token?: string) => { setUser(next); localStorage.setItem("grocers_session", JSON.stringify(next)); if (token) localStorage.setItem("grocers_access_token", token); };
  const login = async (role: Role, identifier: string, password: string) => {
    try {
      const path = role === "CUSTOMER" ? "user" : role.toLowerCase();
      const { data } = await api.post(`/auth/login/${path}`, { email: identifier, password });
      const firstName = data.firstName || "";
      const lastName = data.lastName || "";
      let next = (data.user || { id: tokenSubject(data.accessToken), email: identifier, name: `${firstName} ${lastName}`.trim() || identifier, firstName, lastName, role: roleFromApi(data.role || role), mustChangePassword: data.mustChangePassword }) as SessionUser;
      if (next.locked) { navigate("/support"); toast.error("This account is locked. Please raise a support ticket."); return; }
      save(next, data.accessToken);
      if (next.role === "ADMIN") {
        const profile = await api.get("/admin/me").then(({ data }) => data).catch(() => null);
        if (profile) {
          next = { ...next, id: String(profile.id ?? next.id), firstName: profile.firstName ?? next.firstName, lastName: profile.lastName ?? next.lastName, name: [profile.firstName, profile.lastName].filter(Boolean).join(" ") || next.name, email: profile.email ?? next.email };
          save(next);
        }
      }
      toast.success("Signed in successfully");
      navigate(destination(next));
    } catch (error) { if (role === "CUSTOMER" && axios.isAxiosError(error) && error.response?.status === 403) { toast.error("This account is locked. Send an unlock request to an employee."); navigate(`/unlock-account?email=${encodeURIComponent(identifier)}`); return; } toast.error(errorMessage(error, "Unable to sign in.")); }
  };
  const register = async (payload: Record<string, string>) => {
    try {
      await api.post("/users", payload);
      toast.success("Account created. Please sign in.");
      navigate("/auth");
    } catch (error) {
      toast.error(errorMessage(error, "Unable to create your account."));
    }
  };
  const logout = () => { setUser(null); localStorage.removeItem("grocers_session"); localStorage.removeItem("grocers_access_token"); navigate("/auth"); };
  const updateUser = (value: Partial<SessionUser>) => { if (!user) return; save({ ...user, ...value }); };
  return <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => { const value = useContext(AuthContext); if (!value) throw new Error("useAuth must be used within AuthProvider"); return value; };
