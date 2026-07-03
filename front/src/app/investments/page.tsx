"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ReconciliationBanner } from "@/components/ReconciliationBanner";
import { formatAmount, formatRatio } from "@/lib/format";
import { apiFetch, parseApiResponse } from "@/lib/fetch-client";

type Investment = {
  id: string;
  companyName: string;
  investmentAmount: number;
  investorInvestments: {
    investorName: string;
    allocationRatio: number;
    allocatedAmount: number;
  }[];
};

type Reconciliation = {
  isValid: boolean;
  messages: string[];
  totalFund: number;
  totalInvestment: number;
  cashBalance: number;
};

export default function InvestmentsPage() {
  const [investments, setInvestments] = useState<Investment[]>([]);
  const [reconciliation, setReconciliation] = useState<Reconciliation | null>(
    null,
  );
  const [companyName, setCompanyName] = useState("");
  const [amount, setAmount] = useState("");
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    const [invRes, recRes] = await Promise.all([
      apiFetch("/api/investments"),
      apiFetch("/api/reconciliation"),
    ]);
    const invParsed = await parseApiResponse<{ investments: Investment[] }>(
      invRes,
    );
    const recParsed = await parseApiResponse<Reconciliation>(recRes);
    if (invParsed.ok && invParsed.data) {
      setInvestments(invParsed.data.investments);
    }
    if (recParsed.ok && recParsed.data) {
      setReconciliation(recParsed.data);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    const res = await apiFetch("/api/investments", {
      method: "POST",
      body: JSON.stringify({
        company_name: companyName,
        investment_amount: Number(amount),
      }),
    });

    const parsed = await parseApiResponse(res);
    setLoading(false);

    if (!parsed.ok) {
      setError(parsed.message ?? "요청 처리에 실패했습니다");
      return;
    }

    setSuccess(parsed.message ?? "등록 완료");
    setCompanyName("");
    setAmount("");
    fetchData();
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">기업 투자 관리</h1>
        <p className="mt-1 text-slate-600">
          기업 투자 등록 시 배분비율에 따라 자동 배분됩니다.
        </p>
      </div>

      {reconciliation && (
        <ReconciliationBanner
          isBalanced={reconciliation.isValid}
          messages={[
            ...reconciliation.messages,
            `현금: ${formatAmount(reconciliation.cashBalance)}`,
          ]}
        />
      )}

      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        <h2 className="font-semibold">기업 투자 등록</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <label className="block">
            <span className="text-sm text-slate-600">기업명</span>
            <input
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="기업 X"
            />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600">투자금액 (만 원)</span>
            <input
              required
              type="number"
              min={1}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="300000"
            />
          </label>
        </div>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {success && <p className="mt-3 text-sm text-emerald-600">{success}</p>}
        <button
          type="submit"
          disabled={loading}
          className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading ? "저장 중..." : "투자 등록"}
        </button>
      </form>

      <div className="space-y-4">
        <h2 className="font-semibold">투자 내역 ({investments.length})</h2>
        {investments.length === 0 ? (
          <p className="text-sm text-slate-500">등록된 투자가 없습니다.</p>
        ) : (
          investments.map((inv) => (
            <div
              key={inv.id}
              className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-indigo-700">
                    {inv.companyName}
                  </h3>
                  <p className="text-xs text-slate-500">ID: {inv.id}</p>
                </div>
                <span className="text-lg font-bold">
                  {formatAmount(inv.investmentAmount)}
                </span>
              </div>
              <table className="mt-4 w-full text-sm">
                <thead className="text-left text-slate-500">
                  <tr>
                    <th className="py-2">출자자</th>
                    <th className="py-2">비율</th>
                    <th className="py-2 text-right">투자액</th>
                  </tr>
                </thead>
                <tbody>
                  {inv.investorInvestments.map((ii) => (
                    <tr
                      key={ii.investorName}
                      className="border-t border-slate-100"
                    >
                      <td className="py-2">{ii.investorName}</td>
                      <td className="py-2">{formatRatio(ii.allocationRatio)}</td>
                      <td className="py-2 text-right font-medium">
                        {formatAmount(ii.allocatedAmount)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
