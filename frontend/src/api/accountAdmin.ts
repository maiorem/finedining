import { apiAdminGet } from "./adminHttp";

export type AccountAdmin = {
  id: number;
  nickname: string;
  provider: string;
  locale: "KO" | "EN";
  status: "ACTIVE" | "WITHDRAWN";
  createdAt: string;
};

/** 목록에는 공개 정보만 담긴다 — email·providerUserId는 서버가 아예 내려주지 않는다(CLAUDE.md §3.2·§7.7). */
export function listAccountsForAdmin(accessToken: string): Promise<AccountAdmin[]> {
  return apiAdminGet<AccountAdmin[]>("/api/accounts/manage", accessToken);
}
