"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, parseApiResponse } from "@/lib/fetch-client";

type AuditLog = {
  id: string;
  userId: string;
  action: string;
  entityType: string;
  entityId: string;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
};

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [error, setError] = useState("");

  const fetchLogs = useCallback(async () => {
    const res = await apiFetch("/api/audit-logs");
    const parsed = await parseApiResponse<{ logs: AuditLog[] }>(res);
    if (parsed.ok && parsed.data) {
      setLogs(parsed.data.logs);
      setError("");
    } else {
      setError(parsed.message ?? "조회에 실패했습니다");
    }
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">감시 로그</h1>
        <p className="mt-1 text-slate-600">
          관리자 전용 — 모든 거래 기록을 조회합니다. (NFR-006)
        </p>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-600">
            <tr>
              <th className="px-4 py-3">시각</th>
              <th className="px-4 py-3">사용자</th>
              <th className="px-4 py-3">액션</th>
              <th className="px-4 py-3">엔티티</th>
              <th className="px-4 py-3">변경</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className="border-t border-slate-100">
                <td className="px-4 py-3 whitespace-nowrap">
                  {new Date(log.createdAt).toLocaleString("ko-KR")}
                </td>
                <td className="px-4 py-3">{log.userId}</td>
                <td className="px-4 py-3">{log.action}</td>
                <td className="px-4 py-3">
                  {log.entityType}
                  {log.entityId ? ` (${log.entityId.slice(0, 8)}…)` : ""}
                </td>
                <td className="max-w-xs truncate px-4 py-3 text-slate-500">
                  {log.newValue ?? log.oldValue ?? "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {logs.length === 0 && !error && (
          <p className="px-4 py-8 text-center text-slate-500">
            기록된 로그가 없습니다.
          </p>
        )}
      </div>
    </div>
  );
}
