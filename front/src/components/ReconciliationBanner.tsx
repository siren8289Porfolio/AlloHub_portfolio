type ReconciliationBannerProps = {
  isBalanced: boolean;
  messages: string[];
};

export function ReconciliationBanner({
  isBalanced,
  messages,
}: ReconciliationBannerProps) {
  return (
    <div
      className={`rounded-xl border p-4 ${
        isBalanced
          ? "border-emerald-200 bg-emerald-50"
          : "border-red-200 bg-red-50"
      }`}
    >
      <div className="flex items-center gap-2">
        <span
          className={`inline-flex h-2.5 w-2.5 rounded-full ${
            isBalanced ? "bg-emerald-500" : "bg-red-500"
          }`}
        />
        <h3
          className={`font-semibold ${
            isBalanced ? "text-emerald-800" : "text-red-800"
          }`}
        >
          {isBalanced ? "정합성 검증 통과" : "정합성 오류"}
        </h3>
      </div>
      <ul className="mt-2 space-y-1 text-sm text-slate-700">
        {messages.map((msg) => (
          <li key={msg}>{msg}</li>
        ))}
      </ul>
    </div>
  );
}
