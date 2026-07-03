type StatCardProps = {
  label: string;
  value: string;
  sub?: string;
  variant?: "default" | "success" | "warning";
};

export function StatCard({
  label,
  value,
  sub,
  variant = "default",
}: StatCardProps) {
  const colors = {
    default: "border-slate-200 bg-white",
    success: "border-emerald-200 bg-emerald-50",
    warning: "border-amber-200 bg-amber-50",
  };

  return (
    <div className={`rounded-xl border p-5 shadow-sm ${colors[variant]}`}>
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  );
}
