import { ApiError } from "./http";
import { refreshAdminSession, type AdminSession } from "./auth";

/**
 * 관리자 인증이 필요한 요청 전용 fetch 래퍼. access token은 메모리(AdminAuthContext)에만
 * 있으므로 호출부가 매번 넘겨준다 — 쿠키 자동 전송에 의존하지 않는다 (CLAUDE.md §3.5·§7.4).
 */
type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

let onSessionRefreshed: ((session: AdminSession) => void) | null = null;
let onSessionExpired: (() => void) | null = null;

/**
 * AdminAuthContext가 마운트 시 등록한다. access token은 15분 만료다(§3.4·§7.4) — 만료로
 * 401을 맞으면 이 모듈이 refresh 쿠키로 조용히 재발급받아 재시도하는데, 그 결과를 컨텍스트의
 * 세션 상태에도 반영해야 다음 호출부터 새 토큰을 쓴다. 재발급마저 실패하면(14일 refresh 쿠키도
 * 만료) 세션을 비워 로그인 화면으로 돌려보낸다.
 */
export function registerAdminSessionHandlers(handlers: {
  onRefreshed: (session: AdminSession) => void;
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

/**
 * 401이 아니면 그대로 반환한다. 401이면 딱 한 번만 조용히 재발급 → 재시도한다 — 이게 없으면
 * 편집 패널이 "불러오는 중입니다"에서 영원히 멈춘다(사용자가 실제로 겪은 버그). PIN_REQUIRED는
 * 403이라 이 경로를 타지 않는다 — 기존 PinModal 흐름과 충돌하지 않는다.
 */
async function adminFetch<T>(path: string, init: RequestInit, accessToken: string): Promise<T> {
  const hasBody = init.body !== undefined;
  const response = await fetch(path, { ...init, headers: headers(accessToken, hasBody) });
  if (response.status !== 401) {
    return parseEnvelope<T>(response);
  }

  try {
    const refreshed = await refreshAdminSession();
    onSessionRefreshed?.(refreshed);
    const retryResponse = await fetch(path, { ...init, headers: headers(refreshed.accessToken, hasBody) });
    return parseEnvelope<T>(retryResponse);
  } catch (err) {
    onSessionExpired?.();
    throw err instanceof ApiError ? err : new ApiError("UNAUTHORIZED", "세션이 만료되었습니다. 다시 로그인해 주세요.");
  }
}

export function apiAdminGet<T>(path: string, accessToken: string): Promise<T> {
  return adminFetch<T>(path, { method: "GET" }, accessToken);
}

export function apiAdminPost<T>(path: string, accessToken: string, body?: unknown): Promise<T> {
  return adminFetch<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }, accessToken);
}

export function apiAdminPut<T>(path: string, accessToken: string, body: unknown): Promise<T> {
  return adminFetch<T>(path, { method: "PUT", body: JSON.stringify(body) }, accessToken);
}

export function apiAdminDelete<T>(path: string, accessToken: string): Promise<T> {
  return adminFetch<T>(path, { method: "DELETE" }, accessToken);
}
