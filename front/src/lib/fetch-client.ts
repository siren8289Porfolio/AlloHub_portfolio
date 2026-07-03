"use client";

const TOKEN_KEY = "alloc_auth_token";
const ROLE_KEY = "alloc_auth_role";

export type UserRole = "operator" | "admin";

export function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getAuthRole(): UserRole | null {
  if (typeof window === "undefined") return null;
  const role = sessionStorage.getItem(ROLE_KEY);
  if (role === "operator" || role === "admin") return role;
  return null;
}

export function setAuthSession(token: string, role: UserRole) {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(ROLE_KEY, role);
}

export function clearAuthToken() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(ROLE_KEY);
}

/** EC2 nginx: /allohub/api , 로컬: /api */
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "/api";

function resolveApiUrl(input: RequestInfo): RequestInfo {
  if (typeof input !== "string") return input;
  if (input.startsWith("/api")) {
    return `${API_BASE}${input.slice(4)}`;
  }
  return input;
}

export async function apiFetch(
  input: RequestInfo,
  init?: RequestInit,
): Promise<Response> {
  const token = getAuthToken();
  const headers = new Headers(init?.headers);

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return fetch(resolveApiUrl(input), { ...init, headers });
}

export type ApiSuccess<T> = {
  success: true;
  message: string;
  data: T;
};

export type ApiErrorBody = {
  success: false;
  errorCode: string;
  message: string;
};

export async function parseApiResponse<T>(res: Response): Promise<{
  ok: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
}> {
  const body = await res.json();
  if (body.success === true) {
    return { ok: true, data: body.data, message: body.message };
  }
  return {
    ok: false,
    message: body.message ?? "요청 처리에 실패했습니다",
    errorCode: body.errorCode,
  };
}
