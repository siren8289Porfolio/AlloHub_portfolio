"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ReconciliationBanner } from "@/components/ReconciliationBanner";
import { formatAmount, formatRatio } from "@/lib/format";
import { apiFetch, parseApiResponse } from "@/lib/fetch-client";

type Investor = {
  id: string;
  name: string;
  investmentAmount: number;
  allocationRatio: number;
  cumulativeDistribution: number;
  status: string;
};

type Summary = {
  totalFund: number;
  totalRatio: number;
  ratioValid: boolean;
  count: number;
};

export default function InvestorsPage() {
  const [investors, setInvestors] = useState<Investor[]>([]);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [ratio, setRatio] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const fetchInvestors = useCallback(async () => {
    const res = await apiFetch("/api/investors");
    const parsed = await parseApiResponse<{
      investors: Investor[];
      summary: Summary;
    }>(res);
    if (parsed.ok && parsed.data) {
      setInvestors(parsed.data.investors);
      setSummary(parsed.data.summary);
    }
  }, []);

  useEffect(() => {
    fetchInvestors();
  }, [fetchInvestors]);

  function startEdit(investor: Investor) {
    setEditingId(investor.id);
    setName(investor.name);
    setAmount(String(investor.investmentAmount));
    setRatio(String(investor.allocationRatio));
    setError("");
    setSuccess("");
  }

  function cancelEdit() {
    setEditingId(null);
    setName("");
    setAmount("");
    setRatio("");
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    const payload = {
      investor_name: name,
      investment_amount: Number(amount),
      allocation_ratio: Number(ratio),
    };

    const res = editingId
      ? await apiFetch(`/api/investors/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        })
      : await apiFetch("/api/investors", {
          method: "POST",
          body: JSON.stringify(payload),
        });

    const parsed = await parseApiResponse(res);
    setLoading(false);

    if (!parsed.ok) {
      setError(parsed.message ?? "요청 처리에 실패했습니다");
      return;
    }

    setSuccess(parsed.message ?? "등록 완료");
    cancelEdit();
    fetchInvestors();
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">출자자 관리</h1>
        <p className="mt-1 text-slate-600">
          출자자 정보, 출자금, 배분비율을 등록합니다.
        </p>
      </div>

      {summary && (
        <ReconciliationBanner
          isBalanced={summary.ratioValid}
          messages={[
            summary.ratioValid
              ? `배분비율 합계: ${summary.totalRatio.toFixed(1)}% (100% 이하)`
              : `배분 비율이 100%를 초과합니다 (현재: ${summary.totalRatio.toFixed(1)}%)`,
            `총 출자금: ${formatAmount(summary.totalFund)}`,
          ]}
        />
      )}

      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        <h2 className="font-semibold">
          {editingId ? "출자자 수정" : "출자자 등록"}
        </h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <label className="block">
            <span className="text-sm text-slate-600">출자자명</span>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="출자자 A"
            />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600">출자금액 (만 원)</span>
            <input
              required
              type="number"
              min={1}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="100000"
            />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600">배분비율 (%)</span>
            <input
              required
              type="number"
              min={0.01}
              max={100}
              step={0.01}
              value={ratio}
              onChange={(e) => setRatio(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="20"
            />
          </label>
        </div>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {success && <p className="mt-3 text-sm text-emerald-600">{success}</p>}
        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {loading ? "저장 중..." : editingId ? "수정" : "저장"}
          </button>
          {editingId && (
            <button
              type="button"
              onClick={cancelEdit}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
            >
              취소
            </button>
          )}
        </div>
      </form>

      <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
        <h2 className="border-b border-slate-200 px-6 py-4 font-semibold">
          등록된 출자자 ({investors.length})
        </h2>
        {investors.length === 0 ? (
          <p className="px-6 py-8 text-sm text-slate-500">
            등록된 출자자가 없습니다.
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-6 py-3">출자자명</th>
                <th className="px-6 py-3">출자금액</th>
                <th className="px-6 py-3">배분비율</th>
                <th className="px-6 py-3">누적 배분금</th>
                <th className="px-6 py-3" />
              </tr>
            </thead>
            <tbody>
              {investors.map((inv) => (
                <tr key={inv.id} className="border-t border-slate-100">
                  <td className="px-6 py-3 font-medium">{inv.name}</td>
                  <td className="px-6 py-3">
                    {formatAmount(inv.investmentAmount)}
                  </td>
                  <td className="px-6 py-3">
                    {formatRatio(inv.allocationRatio)}
                  </td>
                  <td className="px-6 py-3">
                    {formatAmount(inv.cumulativeDistribution)}
                  </td>
                  <td className="px-6 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => startEdit(inv)}
                      className="text-indigo-600 hover:underline"
                    >
                      수정
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
