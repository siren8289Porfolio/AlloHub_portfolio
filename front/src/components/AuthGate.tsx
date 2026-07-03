"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  getAuthToken,
  setAuthSession,
  clearAuthToken,
} from "@/lib/fetch-client";

const PRESETS = [
  { label: "운용사", token: "operator-dev-token" },
  { label: "관리자", token: "admin-dev-token" },
];

export function AuthGate({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(false);
  const [token, setToken] = useState("");
  const [error, setError] = useState("");
  const router = useRouter();

  useEffect(() => {
    if (getAuthToken()) {
      setReady(true);
    }
  }, []);

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    setError("");

    const res = await fetch("/api/reconciliation", {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (res.status === 401) {
      setError("인증에 실패했습니다. 토큰을 확인해주세요.");
      return;
    }

    const sessionRes = await fetch("/api/auth/session", {
      headers: { Authorization: `Bearer ${token}` },
    });
    const sessionBody = await sessionRes.json();
    const role = sessionBody.data?.role ?? "operator";

    setAuthSession(token, role);
    setReady(true);
    window.dispatchEvent(new Event("storage"));
    router.refresh();
  }

  function handleLogout() {
    clearAuthToken();
    setReady(false);
    setToken("");
  }

  if (!ready) {
    return (
      <div className="mx-auto max-w-md rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-bold text-indigo-700">AllocHub 로그인</h1>
        <p className="mt-2 text-sm text-slate-600">
          API 토큰으로 인증합니다. (SEC-001)
        </p>
        <form onSubmit={handleLogin} className="mt-6 space-y-4">
          <label className="block">
            <span className="text-sm text-slate-600">API 토큰</span>
            <input
              required
              value={token}
              onChange={(e) => setToken(e.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              placeholder="Bearer 토큰 입력"
            />
          </label>
          <div className="flex gap-2">
            {PRESETS.map((preset) => (
              <button
                key={preset.label}
                type="button"
                onClick={() => setToken(preset.token)}
                className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50"
              >
                {preset.label}
              </button>
            ))}
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            className="w-full rounded-lg bg-indigo-600 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            로그인
          </button>
        </form>
      </div>
    );
  }

  return (
    <>
      <div className="mb-4 flex justify-end">
        <button
          type="button"
          onClick={handleLogout}
          className="text-sm text-slate-500 hover:text-slate-700"
        >
          로그아웃
        </button>
      </div>
      {children}
    </>
  );
}
