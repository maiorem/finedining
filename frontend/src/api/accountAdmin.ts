import { apiAdminGet } from "./adminHttp";

export type AccountAdmin = {
  id: number;
  nickname: string;
  email: string | null;
  provider: string;
  locale: "KO" | "EN";
  status: "ACTIVE" | "WITHDRAWN";
  createdAt: string;
};

/**
 * email은 2026-09-12 결정으로 포함했다 — providerUserId(카카오 내부 식별자)는 여전히
 * 서버가 내려주지 않는다(CLAUDE.md §3.2·§7.7). 개인정보처리방침에 "관리자의 회원관리 열람"
 * 목적을 명시하는 문서 작업이 오픈 전 별도로 필요하다(§15).
 */
export function listAccountsForAdmin(accessToken: string): Promise<AccountAdmin[]> {
  return apiAdminGet<AccountAdmin[]>("/api/accounts/manage", accessToken);
}
