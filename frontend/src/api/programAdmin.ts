import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";

export type ProgramTranslationView = {
  locale: "KO" | "EN";
  title: string | null;
  description: string | null;
  draftTitle: string | null;
  draftDescription: string | null;
  hasPendingDraft: boolean;
};

export type ProgramAdmin = {
  id: number;
  status: "DRAFT" | "PUBLISHED";
  applyUrl: string | null;
  locationUrl: string | null;
  translations: ProgramTranslationView[];
};

export function listProgramsForAdmin(accessToken: string): Promise<ProgramAdmin[]> {
  return apiAdminGet<ProgramAdmin[]>("/api/programs/manage", accessToken);
}

export function getProgramForAdmin(accessToken: string, id: number): Promise<ProgramAdmin> {
  return apiAdminGet<ProgramAdmin>(`/api/programs/manage/${id}`, accessToken);
}

/** 목록에서 "새 프로그램 추가"로 호출한다. 본문 없이 DRAFT로 만든다(CLAUDE.md §3.9). */
export function createProgram(accessToken: string): Promise<ProgramAdmin> {
  return apiAdminPost<ProgramAdmin>("/api/programs", accessToken);
}

/** "임시저장" — 공개본에는 영향이 없다(CLAUDE.md §3.9). */
export function saveProgramDraftTranslation(
  accessToken: string,
  id: number,
  locale: "KO" | "EN",
  title: string,
  description: string | null,
): Promise<ProgramAdmin> {
  return apiAdminPut<ProgramAdmin>(`/api/programs/${id}/translations/${locale}`, accessToken, {
    title,
    description,
  });
}

export function changeProgramApplyUrl(accessToken: string, id: number, url: string | null): Promise<ProgramAdmin> {
  return apiAdminPut<ProgramAdmin>(`/api/programs/${id}/apply-url`, accessToken, { url });
}

export function changeProgramLocationUrl(accessToken: string, id: number, url: string | null): Promise<ProgramAdmin> {
  return apiAdminPut<ProgramAdmin>(`/api/programs/${id}/location-url`, accessToken, { url });
}

export function publishProgram(accessToken: string, id: number): Promise<ProgramAdmin> {
  return apiAdminPost<ProgramAdmin>(`/api/programs/${id}/publish`, accessToken);
}

export function unpublishProgram(accessToken: string, id: number): Promise<ProgramAdmin> {
  return apiAdminPost<ProgramAdmin>(`/api/programs/${id}/unpublish`, accessToken);
}
