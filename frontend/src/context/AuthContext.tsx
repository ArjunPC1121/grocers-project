import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import api, { errorMessage } from "../config/api";
import type { Role, SessionUser } from "../types";
type AuthContextType = { user: SessionUser | null; loading: boolean; login: (role: Role, identifier: string, password: string) => Promise<void>; register: (payload: Record<string, string>) => Promise<void>; logout: () => void; updateUser: (value: Partial<SessionUser>) => void; };
const AuthContext = createContext<AuthContextType | undefined>(undefined);
const destination = (user: SessionUser) => user.mustChangePassword ? "/employee/profile" : user.role === "ADMIN" ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/";
export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate(); const [user, setUser] = useState<SessionUser | null>(null); const [loading, setLoading] = useState(true);
  useEffect(() => { try { const saved = localStorage.getItem("grocers_session"); if (saved) setUser(JSON.parse(saved)); } catch { localStorage.removeItem("grocers_session"); } finally { setLoading(false); } }, []);
  const save = (next: SessionUser, token?: string) => { setUser(next); localStorage.setItem("grocers_session", JSON.stringify(next)); if (token) localStorage.setItem("grocers_access_token", token); };
  const login = async (role: Role, identifier: string, password: string) => { try { const { data } = await api.post("/auth/login", { role, identifier, password }); const next = data.user as SessionUser; if (next.locked) { navigate("/support"); toast.error("This account is locked. Please raise a support ticket."); return; } save(next, data.accessToken); toast.success("Signed in successfully"); navigate(destination(next)); } catch (error) { toast.error(errorMessage(error, "Unable to sign in.")); } };
  const register = async (payload: Record<string, string>) => { try { const { data } = await api.post("/auth/register", payload); save(data.user, data.accessToken); toast.success("Your Grocers account is ready"); navigate("/"); } catch (error) { toast.error(errorMessage(error, "Unable to create your account.")); } };
  const logout = () => { setUser(null); localStorage.removeItem("grocers_session"); localStorage.removeItem("grocers_access_token"); navigate("/auth"); };
  const updateUser = (value: Partial<SessionUser>) => { if (!user) return; save({ ...user, ...value }); };
  return <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => { const value = useContext(AuthContext); if (!value) throw new Error("useAuth must be used within AuthProvider"); return value; };
