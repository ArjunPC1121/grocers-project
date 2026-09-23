import { useState } from "react";
import toast from "react-hot-toast";
import api, { errorMessage } from "../../config/api";
import { useAuth } from "../../context/AuthContext";
import { meetsEmployeePasswordRequirements, PasswordRequirements } from "../../components/PasswordRequirements";

export default function EmployeePasswordChange() {
  const { user, logout } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!currentPassword) return toast.error("Enter your current password.");
    if (!meetsEmployeePasswordRequirements(newPassword)) return toast.error("Complete all password requirements.");
    if (newPassword !== confirmPassword) return toast.error("The passwords do not match.");
    if (!user) return;
    setSubmitting(true);
    try {
      await api.put(`/employees/${user.id}/password`, { currentPassword, newPassword, confirmPassword });
      toast.success("Password updated. Please sign in with your new password.");
      logout();
    } catch (error) {
      toast.error(errorMessage(error, "Unable to update your password."));
    } finally {
      setSubmitting(false);
    }
  };

  const ready = Boolean(currentPassword) && meetsEmployeePasswordRequirements(newPassword) && newPassword === confirmPassword;
  return <main className="min-h-screen bg-app-cream p-6 flex-center"><section className="w-full max-w-md rounded-2xl border bg-white p-8 shadow-sm"><p className="font-semibold text-app-orange">EMPLOYEE ACCOUNT</p><h1 className="mt-2 font-serif text-3xl">Set a new password</h1><p className="mt-3 text-sm text-app-text-light">For security, confirm your current password before accessing the employee workspace.</p><form onSubmit={submit} className="mt-7 space-y-4"><label className="block text-sm font-medium">Current password<input required type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} className="mt-1 w-full rounded-xl border border-app-border p-3" /></label><label className="block text-sm font-medium">New password<input required minLength={8} type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} className="mt-1 w-full rounded-xl border border-app-border p-3" /><PasswordRequirements password={newPassword} confirmation={confirmPassword}/></label><label className="block text-sm font-medium">Re-enter new password<input required minLength={8} type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} className="mt-1 w-full rounded-xl border border-app-border p-3" /></label><button disabled={submitting || !ready} className="w-full rounded-xl bg-app-green p-3 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60">{submitting ? "Updating password..." : "Save new password"}</button></form><button type="button" onClick={logout} className="mt-4 w-full text-sm text-app-orange">Sign out</button></section></main>;
}
