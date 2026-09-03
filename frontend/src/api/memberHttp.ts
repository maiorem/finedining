import { ApiError } from "./http";
import { refreshMemberSession, type MemberSession } from "./memberAuth";

/**
 * 회원 인증이 필요한 요청 전용 fetch 래퍼. adminHttp.ts와 동일한 패턴이다 — access token은
 * 메모리(MemberAuthContext)에만 있으므로 호출부가 매번 넘겨준다(CLAUDE.md §3.5·§7.4).
 */
type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

let onSessionRefreshed: ((session: MemberSession) => void) | null = null;
let onSessionExpired: (() => void) | null = null;

/**
 * MemberAuthContext가 마운트 시 등록한다. access token 만료로 401을 맞으면 이 모듈이 refresh
 * 쿠키로 조용히 재발급받아 재시도한다 — adminHttp.ts와 동일한 이유(§7.4).
 */
export function registerMemberSessionHandlers(handlers: {
  onRefreshed: (session: MemberSession) => void;
  onExpired: () => void;
}) {
  onSessionRefreshed = handlers.onRefreshed;
  onSessionExpired = handlers.onExpired;
}

function headers(accessToken: string, hasBody: boolean): HeadersInit {
  const base: Record<string, string> = { Authorization: `Bearer ${accessToken}` };
  if (hasBody) {
    base["Content-Type"] = "application/json";
  }
  return base;
}

async function parseEnvelope<T>(response: Response): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T>;
  if (!body.success) {
    throw new ApiError(body.error?.code ?? "UNKNOWN", body.error?.message ?? "요청이 실패했습니다.");
  }
  return body.data as T;
}

async function memberFetch<T>(path: string, init: RequestInit, accessToken: string): Promise<T> {
  const hasBody = init.body !== undefined;
  const response = await fetch(path, { ...init, headers: headers(accessToken, hasBody) });
  if (response.status !== 401) {
    return parseEnvelope<T>(response);
  }

  try {
    const refreshed = await refreshMemberSession();
    onSessionRefreshed?.(refreshed);
    const retryResponse = await fetch(path, { ...init, headers: headers(refreshed.accessToken, hasBody) });
    return parseEnvelope<T>(retryResponse);
  } catch (err) {
    onSessionExpired?.();
    throw err instanceof ApiError ? err : new ApiError("UNAUTHORIZED", "세션이 만료되었습니다. 다시 로그인해 주세요.");
  }
}

export function apiMemberPost<T>(path: string, accessToken: string, body?: unknown): Promise<T> {
  return memberFetch<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }, accessToken);
}

export function apiMemberPut<T>(path: string, accessToken: string, body: unknown): Promise<T> {
  return memberFetch<T>(path, { method: "PUT", body: JSON.stringify(body) }, accessToken);
}

export function apiMemberDelete<T>(path: string, accessToken: string): Promise<T> {
  return memberFetch<T>(path, { method: "DELETE" }, accessToken);
}
