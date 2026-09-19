import { apiAdminPut } from "./adminHttp";
import { apiGet } from "./http";

export type SiteStatus = { open: boolean };

/** 비공개여도 누구나 호출할 수 있다 — 프론트가 "준비 중" 화면을 띄울지 정하려고 부팅 때 묻는다. */
export function getSiteStatus(): Promise<SiteStatus> {
  return apiGet<SiteStatus>("/api/site/status");
}

/** SUPER_ADMIN 전용이고 PIN 확인(PIN_REQUIRED)이 필요하다(CLAUDE.md §3.4). */
export function setSiteOpen(accessToken: string, open: boolean): Promise<SiteStatus> {
  return apiAdminPut<SiteStatus>("/api/site/visibility", accessToken, { open });
}
