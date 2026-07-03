"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { StatCard } from "@/components/StatCard";
import { ReconciliationBanner } from "@/components/ReconciliationBanner";
import { apiFetch, parseApiResponse } from "@/lib/fetch-client";
import { formatAmount } from "@/lib/format";

type Reconciliation = {
  totalFund: number;
  totalInvestment: number;
  cashBalance: number;
  totalRatio: number;
  totalDistribution: number;
  isValid: boolean;
  messages: string[];
};

export default function DashboardPage() {
  const [reconciliation, setReconciliation] = useState<Reconciliation | null>(
    null,
  );
  const [investorCount, setInvestorCount] = useState(0);
  const [investmentCount, setInvestmentCount] = useState(0);
  const [distributionCount, setDistributionCount] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    const [reconRes, invRes, investRes, distRes] = await Promise.all([
      apiFetch("/api/reconciliation"),
      apiFetch("/api/investors"),
      apiFetch("/api/investments"),
      apiFetch("/api/distributions"),
    ]);

    const recon = await parseApiResponse<Reconciliation>(reconRes);
    const inv = await parseApiResponse<{
      investors: unknown[];
      summary: { count: number };
    }>(invRes);
    const invest = await parseApiResponse<{ investments: unknown[] }>(
      investRes,
    );
    const dist = await parseApiResponse<{ distributions: unknown[] }>(distRes);

    if (recon.ok && recon.data) setReconciliation(recon.data);
    if (inv.ok && inv.data) setInvestorCount(inv.data.summary.count);
    if (invest.ok && invest.data)
      setInvestmentCount(invest.data.investments.length);
    if (dist.ok && dist.data)
      setDistributionCount(dist.data.distributions.length);
    setLoading(false);
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  if (loading || !reconciliation) {
    return (
      <div className="py-12 text-center text-slate-500">대시보드 로딩 중...</div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">대시보드</h1>
        <p className="mt-1 text-slate-600">
          출자금, 투자금, 현금, 배분금 현황을 한눈에 확인합니다.
        </p>
      </div>

      <ReconciliationBanner
        isBalanced={reconciliation.isValid}
        messages={reconciliation.messages}
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="총 출자금"
          value={formatAmount(reconciliation.totalFund)}
          sub={`출자자 ${investorCount}명 · 비율 ${reconciliation.totalRatio.toFixed(1)}%`}
        />
        <StatCard
          label="총 투자금"
          value={formatAmount(reconciliation.totalInvestment)}
          sub={`투자 건수 ${investmentCount}건`}
        />
        <StatCard
          label="현금"
          value={formatAmount(reconciliation.cashBalance)}
          sub="출자금 − 투자금"
          variant={reconciliation.isValid ? "default" : "warning"}
        />
        <StatCard
          label="총 배분금"
          value={formatAmount(reconciliation.totalDistribution)}
          sub={`배분 건수 ${distributionCount}건`}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Link
          href="/investors"
          className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm transition hover:border-indigo-300 hover:shadow-md"
        >
          <h2 className="font-semibold text-indigo-700">출자자 관리</h2>
          <p className="mt-2 text-sm text-slate-600">
            출자자 등록 및 배분비율 검증
          </p>
        </Link>
        <Link
          href="/investments"
          className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm transition hover:border-indigo-300 hover:shadow-md"
        >
          <h2 className="font-semibold text-indigo-700">기업 투자 관리</h2>
          <p className="mt-2 text-sm text-slate-600">
            투자 등록 및 출자자별 자동 배분
          </p>
        </Link>
        <Link
          href="/distributions"
          className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm transition hover:border-indigo-300 hover:shadow-md"
        >
          <h2 className="font-semibold text-indigo-700">배분금 관리</h2>
          <p className="mt-2 text-sm text-slate-600">
            배당금/회수금 자동 계산 및 배분
          </p>
        </Link>
      </div>
    </div>
  );
}
