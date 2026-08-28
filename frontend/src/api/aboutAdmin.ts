import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";

export type AboutTranslationView = {
  locale: "KO" | "EN";
  intro: string | null;
  draftIntro: string | null;
  hasPendingDraft: boolean;
};

export type AboutAdmin = {
  id: number;
  status: "DRAFT" | "PUBLISHED";
  translations: AboutTranslationView[];
};

// 싱글턴이다 — 생성 API가 없다. Flyway가 심은 행 하나만 계속 조회·편집한다(CLAUDE.md §1·§6).
export function getAboutForAdmin(accessToken: string): Promise<AboutAdmin> {
  return apiAdminGet<AboutAdmin>("/api/about/manage", accessToken);
}

/** "임시저장" — 공개본에는 영향이 없다(CLAUDE.md §3.9). */
export function saveAboutDraftTranslation(
  accessToken: string,
  locale: "KO" | "EN",
  intro: string,
): Promise<AboutAdmin> {
  return apiAdminPut<AboutAdmin>(`/api/about/translations/${locale}`, accessToken, { intro });
}

export function publishAbout(accessToken: string): Promise<AboutAdmin> {
  return apiAdminPost<AboutAdmin>("/api/about/publish", accessToken);
}

export function unpublishAbout(accessToken: string): Promise<AboutAdmin> {
  return apiAdminPost<AboutAdmin>("/api/about/unpublish", accessToken);
}
