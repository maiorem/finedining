import { ApiError } from "./http";

export type AdminRole = "EDITOR" | "SUPER_ADMIN";

export type AdminSession = {
  accessToken: string;
  username: string;
  role: AdminRole;
};

type ApiEnvelope<T> = {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
};

// 관리자 로그인은 카카오 OAuth2와 완전히 별개인 아이디·비밀번호 흐름이다 (CLAUDE.md §3.1·§7.4).
async function postAdminAuth(path: "login" | "refresh" | "logout", body?: unknown): Promise<AdminSession> {
  const response = await fetch(`/api/auth/admin/${path}`, {
    method: "POST",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const envelope = (await response.json()) as ApiEnvelope<AdminSession>;

  if (!envelope.success) {
    throw new ApiError(envelope.error?.code ?? "UNKNOWN", envelope.error?.message ?? "요청이 실패했습니다.");
  }

  return envelope.data as AdminSession;
}

export function loginAdmin(username: string, password: string): Promise<AdminSession> {
  return postAdminAuth("login", { username, password });
}

/** 페이지 로드 시 refresh 쿠키로 세션을 조용히 복구한다. access token은 메모리에만 둔다(§7.4). */
export function refreshAdminSession(): Promise<AdminSession> {
  return postAdminAuth("refresh");
}

export async function logoutAdmin(): Promise<void> {
  await fetch("/api/auth/admin/logout", { method: "POST" });
}
