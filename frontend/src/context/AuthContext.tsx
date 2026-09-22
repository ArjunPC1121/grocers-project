import {createContext, useContext, useEffect, useState, type ReactNode} from "react";
import {useNavigate} from "react-router-dom";
import toast from "react-hot-toast";
import api, {errorMessage} from "../config/api";
import type {Role, SessionUser} from "../types";

type AuthContextType = {
    user: SessionUser | null;
    loading: boolean;
    login: (role: Role, identifier: string, password: string, returnTo?: string) => Promise<void>;
    register: (payload: Record<string, string>) => Promise<boolean>;
    logout: () => void;
    updateUser: (value: Partial<SessionUser>) => void;
};
type LoginResponse = {
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
    role: "USER" | "EMPLOYEE" | "ADMIN";
    mustChangePassword: boolean;
    message: string;
};

type JwtClaims = {
    sub: string;
    email: string;
    role: string;
    mustChangePassword?: boolean;
};

type CustomerProfileResponse = {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    dob: string;
    phoneNumber: string;
    address: string;
    accountNumber: string;
    funds: number;
};

const readJwtClaims = (token: string): JwtClaims => {
    const encodedPayload = token.split(".")[1];

    if (!encodedPayload) {
        throw new Error("The login response contains an invalid access token.");
    }

    const base64 = encodedPayload
        .replace(/-/g, "+")
        .replace(/_/g, "/");

    return JSON.parse(window.atob(base64)) as JwtClaims;
};
const AuthContext = createContext<AuthContextType | undefined>(undefined);
const destination = (user: SessionUser) => user.mustChangePassword ? "/employee/profile" : user.role === "ADMIN" ? "/admin" : user.role === "EMPLOYEE" ? "/employee" : "/";

export function AuthProvider({children}: { children: ReactNode }) {
    const navigate = useNavigate();
    const [user, setUser] = useState<SessionUser | null>(null);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        try {
            const saved = localStorage.getItem("grocers_session");
            if (saved) setUser(JSON.parse(saved));
        } catch {
            localStorage.removeItem("grocers_session");
        } finally {
            setLoading(false);
        }
    }, []);
    const save = (next: SessionUser, token?: string) => {
        setUser(next);
        localStorage.setItem("grocers_session", JSON.stringify(next));
        if (token) localStorage.setItem("grocers_access_token", token);
    };
    const login = async (
        role: Role,
        identifier: string,
        password: string,
        returnTo?: string,
    ) => {
        try {
            if (role !== "CUSTOMER") {
                toast.error("Only customer login is being connected right now.");
                return;
            }

            const {data} = await api.post<LoginResponse>("/auth/login/user", {
                email: identifier,
                password,
            });

            const claims = readJwtClaims(data.accessToken);

            // This lets Axios authorize the profile request below.
            localStorage.setItem("grocers_access_token", data.accessToken);

            const {data: profile} = await api.get<CustomerProfileResponse>(
                `/users/${claims.sub}`,
            );

            const next: SessionUser = {
                id: String(profile.id),
                firstName: profile.firstName,
                lastName: profile.lastName,
                name: `${profile.firstName} ${profile.lastName}`,
                email: profile.email,
                phone: profile.phoneNumber,
                role: "CUSTOMER",
            };

            save(next, data.accessToken);
            toast.success("Signed in successfully");
            const safeReturnTo = returnTo?.startsWith("/") && !returnTo.startsWith("//")
                ? returnTo
                : undefined;
            navigate(safeReturnTo || destination(next), {replace: true});
        } catch (error) {
            localStorage.removeItem("grocers_access_token");
            toast.error(errorMessage(error, "Unable to sign in."));
        }
    };
    const register = async (payload: Record<string, string>) => {
        try {
            await api.post("/users", payload);
            toast.success("Account created. Please sign in.");
            return true;
        } catch (error) {
            toast.error(errorMessage(error, "Unable to create your account."));
            return false;
        }
    };
    const logout = () => {
        setUser(null);
        localStorage.removeItem("grocers_session");
        localStorage.removeItem("grocers_access_token");
        navigate("/auth");
    };
    const updateUser = (value: Partial<SessionUser>) => {
        if (!user) return;
        save({...user, ...value});
    };
    return <AuthContext.Provider
        value={{user, loading, login, register, logout, updateUser}}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
    const value = useContext(AuthContext);
    if (!value) throw new Error("useAuth must be used within AuthProvider");
    return value;
};
