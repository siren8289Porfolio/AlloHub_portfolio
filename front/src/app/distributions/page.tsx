"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { formatAmount, formatRatio } from "@/lib/format";
import { apiFetch, parseApiResponse } from "@/lib/fetch-client";

type CalcDetail = {
  investorId: string;
  investorName: string;
  allocationRatio: number;
  distributedAmount: number;
};

type InvestmentOption = {
  id: string;
  companyName: string;
  investmentAmount: number;
};

type Distribution = {
  id: string;
  distributionAmount: number;
  distributionType: string;
  investment: { id: string; companyName: string };
  details: {
    distributedAmount: number;
    investor: { name: string; allocationRatio: number };
  }[];
};

export default function DistributionsPage() {
  const [distributions, setDistributions] = useState<Distribution[]>([]);
  const [investments, setInvestments] = useState<InvestmentOption[]>([]);
  const [amount, setAmount] = useState("");
  const [type, setType] = useState("배당금");
  const [investmentId, setInvestmentId] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<{
    details: CalcDetail[];
    isValid: boolean;
    totalDistributed: number;
  } | null>(null);
  const [calculating, setCalculating] = useState(false);

  const fetchData = useCallback(async () => {
    const [distRes, invRes] = await Promise.all([
      apiFetch("/api/distributions"),
      apiFetch("/api/investments"),
    ]);
    const distParsed = await parseApiResponse<{ distributions: Distribution[] }>(
      distRes,
    );
    const invParsed = await parseApiResponse<{ investments: InvestmentOption[] }>(
      invRes,
    );
    if (distParsed.ok && distParsed.data) {
      setDistributions(distParsed.data.distributions);
    }
    if (invParsed.ok && invParsed.data) {
      const list = invParsed.data.investments;
      setInvestments(list);
      if (list.length > 0 && !investmentId) {
        setInvestmentId(list[0].id);
      }
    }
  }, [investmentId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  function buildPayload() {
    return {
      distribution_amount: Number(amount),
      distribution_type: type,
      investment_id: investmentId,
    };
  }

  async function handleCalculate() {
    setError("");
    setCalculating(true);
    const res = await apiFetch("/api/distributions/calculate", {
      method: "POST",
      body: JSON.stringify(buildPayload()),
    });
    const parsed = await parseApiResponse<{
      details: CalcDetail[];
      isValid: boolean;
      totalDistributed: number;
    }>(res);
    setCalculating(false);

    if (!parsed.ok) {
      setError(parsed.message ?? "계산에 실패했습니다");
      setPreview(null);
      return;
    }

    setPreview(parsed.data ?? null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    const res = await apiFetch("/api/distributions", {
      method: "POST",
      body: JSON.stringify(buildPayload()),
    });

    const parsed = await parseApiResponse(res);
    setLoading(false);

    if (!parsed.ok) {
      setError(parsed.message ?? "요청 처리에 실패했습니다");
      return;
    }

    setSuccess(parsed.message ?? "등록 완료");
    setAmount("");
    setPreview(null);
    fetchData();
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">배분금 관리</h1>
        <p className="mt-1 text-slate-600">
          배당금/회수금을 배분비율에 따라 자동 배분합니다.
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        <h2 className="font-semibold">배분금 등록</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="block">
            <span className="text-sm text-slate-600">투자 건 (investment_id)</span>
            <select
              required
              value={investmentId}
              onChange={(e) => setInvestmentId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            >
              {investments.length === 0 ? (
                <option value="">투자 등록 후 선택 가능</option>
              ) : (
                investments.map((inv) => (
                  <option key={inv.id} value={inv.id}>
                    {inv.companyName} ({formatAmount(inv.investmentAmount)})
                  </option>
                ))
              )}
            </select>
          </label>
          <label className="block">
            <span className="text-sm text-slate-600">유형</span>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            >
              <option value="배당금">배당금</option>
              <option value="회수금">회수금</option>
            </select>
          </label>
          <label className="block">
            <span className="text-sm text-slate-600">배분금액 (만 원)</span>
            <input
              required
              type="number"
              min={1}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="60000"
            />
          </label>
          <div className="flex items-end gap-2">
            <button
              type="button"
              onClick={handleCalculate}
              disabled={calculating || !amount || !investmentId}
              className="rounded-lg border border-indigo-300 px-4 py-2 text-sm font-medium text-indigo-700 hover:bg-indigo-50 disabled:opacity-50"
            >
              {calculating ? "계산 중..." : "자동 계산"}
            </button>
            <button
              type="submit"
              disabled={loading || investments.length === 0}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? "저장 중..." : "저장"}
            </button>
          </div>
        </div>
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {success && <p className="mt-3 text-sm text-emerald-600">{success}</p>}

        {preview && (
          <div className="mt-6 rounded-lg border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-center justify-between">
              <h3 className="font-medium">자동 계산 결과</h3>
              <span
                className={`text-sm font-medium ${
                  preview.isValid ? "text-emerald-600" : "text-red-600"
                }`}
              >
                {preview.isValid ? "정합성 통과" : "정합성 오류"}
              </span>
            </div>
            <table className="mt-3 w-full text-sm">
              <thead className="text-left text-slate-500">
                <tr>
                  <th className="py-1">출자자</th>
                  <th className="py-1">비율</th>
                  <th className="py-1 text-right">배분액</th>
                </tr>
              </thead>
              <tbody>
                {preview.details.map((d) => (
                  <tr key={d.investorId} className="border-t border-slate-200">
                    <td className="py-2">{d.investorName}</td>
                    <td className="py-2">{formatRatio(d.allocationRatio)}</td>
                    <td className="py-2 text-right font-medium">
                      {formatAmount(d.distributedAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t border-slate-300 font-semibold">
                  <td className="py-2" colSpan={2}>
                    합계
                  </td>
                  <td className="py-2 text-right">
                    {formatAmount(preview.totalDistributed)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </form>

      <div className="space-y-4">
        <h2 className="font-semibold">배분 내역 ({distributions.length})</h2>
        {distributions.length === 0 ? (
          <p className="text-sm text-slate-500">등록된 배분금이 없습니다.</p>
        ) : (
          distributions.map((dist) => (
            <div
              key={dist.id}
              className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <div>
                  <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-medium text-indigo-700">
                    {dist.distributionType}
                  </span>
                  <h3 className="mt-1 font-semibold">
                    {dist.investment.companyName}
                  </h3>
                </div>
                <span className="text-lg font-bold">
                  {formatAmount(dist.distributionAmount)}
                </span>
              </div>
              <table className="mt-4 w-full text-sm">
                <thead className="text-left text-slate-500">
                  <tr>
                    <th className="py-2">출자자</th>
                    <th className="py-2">비율</th>
                    <th className="py-2 text-right">배분액</th>
                  </tr>
                </thead>
                <tbody>
                  {dist.details.map((d) => (
                    <tr
                      key={d.investor.name}
                      className="border-t border-slate-100"
                    >
                      <td className="py-2">{d.investor.name}</td>
                      <td className="py-2">
                        {formatRatio(d.investor.allocationRatio)}
                      </td>
                      <td className="py-2 text-right font-medium">
                        {formatAmount(d.distributedAmount)}
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
