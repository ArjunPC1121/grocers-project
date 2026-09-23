type PasswordRequirementsProps = { password: string; confirmation?: string };

export const meetsEmployeePasswordRequirements = (password: string) =>
  /^(?=.*[A-Z])(?=.*[^\w\s]).{8,}$/.test(password);

export function PasswordRequirements({ password, confirmation }: PasswordRequirementsProps) {
  const checks = [
    [password.length >= 8, "At least 8 characters"],
    [/[A-Z]/.test(password), "At least one uppercase letter"],
    [/[^\w\s]/.test(password), "At least one symbol"],
    [confirmation === undefined || (confirmation.length > 0 && password === confirmation), "New passwords match"],
  ] as const;
  return <ul className="mt-2 space-y-1 text-xs" aria-live="polite">{checks.map(([passed, label]) => <li key={label} className={passed ? "text-emerald-600" : "text-slate-500"}>{passed ? "✓" : "○"} {label}</li>)}</ul>;
}
