import { useState } from "react";
import { ShoppingBasket, ShieldCheck, UserRoundCog } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import type { Role } from "../types";

const roles: Array<{ role: Role; label: string; detail: string; icon: typeof ShoppingBasket }> = [
  { role: "CUSTOMER", label: "Customer", detail: "Shop, manage funds, orders and support", icon: ShoppingBasket },
  { role: "EMPLOYEE", label: "Employee", detail: "Inventory requests, orders and tickets", icon: UserRoundCog },
  { role: "ADMIN", label: "Admin", detail: "Catalog, people, requests and reports", icon: ShieldCheck },
];
export default function AuthPortal() {
  const { login, register } = useAuth(); const [role, setRole] = useState<Role | null>(null); const [registering, setRegistering] = useState(false); const [form, setForm] = useState<Record<string,string>>({});
  const update = (key: string, value: string) => setForm({ ...form, [key]: value });
  if (!role) return <main className="min-h-screen bg-app-cream p-6 flex-center"><section className="w-full max-w-4xl"><p className="text-app-orange font-semibold">GROCERS</p><h1 className="mt-2 text-4xl font-serif">Choose your workspace</h1><p className="mt-3 text-app-text-light">One grocery platform, tailored for every role.</p><div className="mt-8 grid gap-5 md:grid-cols-3">{roles.map(({ role: value, label, detail, icon: Icon }) => <button key={value} onClick={() => setRole(value)} className="text-left rounded-2xl border bg-white p-6 hover:border-app-orange hover:shadow-md"><Icon className="text-app-orange" /><h2 className="mt-5 font-semibold text-xl">{label}</h2><p className="mt-2 text-sm text-app-text-light">{detail}</p></button>)}</div></section></main>;
  const customerRegistration = role === "CUSTOMER" && registering;
  const fields = customerRegistration ? [["firstName","First name"],["lastName","Last name"],["email","Email"],["password","Password"],["dob","Date of birth"],["phone","Phone"],["address","Address"]] : [[role === "EMPLOYEE" ? "employeeId" : "email", role === "EMPLOYEE" ? "Employee ID" : "Email"],["password","Password"]];
  return <main className="min-h-screen bg-app-cream p-6 flex-center"><form onSubmit={(event) => { event.preventDefault(); if (customerRegistration) void register(form); else void login(role, form.employeeId || form.email || "", form.password || ""); }} className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm border"><button type="button" className="text-sm text-app-orange" onClick={() => setRole(null)}>← Change role</button><p className="mt-6 text-app-orange font-semibold">{role}</p><h1 className="mt-1 text-3xl font-serif">{customerRegistration ? "Create your account" : "Sign in to Grocers"}</h1>{fields.map(([key,label]) => <label key={key} className="block mt-4 text-sm font-medium">{label}<input required type={key === "password" ? "password" : key === "dob" ? "date" : "text"} value={form[key] || ""} onChange={(e) => update(key,e.target.value)} className="mt-1 w-full rounded-xl border border-app-border p-3" /></label>)}<button className="mt-6 w-full rounded-xl bg-app-green p-3 text-white font-semibold">{customerRegistration ? "Create account" : "Sign in"}</button>{role === "CUSTOMER" && <button type="button" onClick={() => setRegistering(!registering)} className="mt-4 w-full text-sm text-app-orange">{customerRegistration ? "Already have an account? Sign in" : "New to Grocers? Create an account"}</button>}</form></main>;
}
