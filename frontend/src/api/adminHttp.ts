import { ApiError } from "./http";

/**
 * 관리자 인증이 필요한 요청 전용 fetch 래퍼. access token은 메모리(AdminAuthContext)에만
 * 있으므로 호출부가 매번 넘겨준다 — 쿠키 자동 전송에 의존하지 않는다 (CLAUDE.md §3.5·§7.4).
 */
type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

async function parseEnvelope<T>(response: Response): Promise<T> {
  const body = (await response.json()) as ApiEnvelope<T>;
  if (!body.success) {
    throw new ApiError(body.error?.code ?? "UNKNOWN", body.error?.message ?? "요청이 실패했습니다.");
  }
  return body.data as T;
}

function headers(accessToken: string, hasBody: boolean): HeadersInit {
  const base: Record<string, string> = { Authorization: `Bearer ${accessToken}` };
  if (hasBody) {
    base["Content-Type"] = "application/json";
  }
  return base;
}

export async function apiAdminGet<T>(path: string, accessToken: string): Promise<T> {
  const response = await fetch(path, { headers: headers(accessToken, false) });
  return parseEnvelope<T>(response);
}

export async function apiAdminPost<T>(path: string, accessToken: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "POST",
    headers: headers(accessToken, body !== undefined),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return parseEnvelope<T>(response);
}

export async function apiAdminPut<T>(path: string, accessToken: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: "PUT",
    headers: headers(accessToken, true),
    body: JSON.stringify(body),
  });
  return parseEnvelope<T>(response);
}

export async function apiAdminDelete<T>(path: string, accessToken: string): Promise<T> {
  const response = await fetch(path, { method: "DELETE", headers: headers(accessToken, false) });
  return parseEnvelope<T>(response);
}
