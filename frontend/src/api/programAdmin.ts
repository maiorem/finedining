import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

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
  slug: string;
  status: "DRAFT" | "PUBLISHED";
  applyUrl: string | null;
  locationUrl: string | null;
  translations: ProgramTranslationView[];
  images: MediaAsset[];
};

export function listProgramsForAdmin(accessToken: string): Promise<ProgramAdmin[]> {
  return apiAdminGet<ProgramAdmin[]>("/api/programs/manage", accessToken);
}

export function getProgramForAdmin(accessToken: string, id: number): Promise<ProgramAdmin> {
  return apiAdminGet<ProgramAdmin>(`/api/programs/manage/${id}`, accessToken);
}

/** 목록에서 "새 프로그램 추가"로 호출한다. 슬러그만 받아 DRAFT로 만든다(CLAUDE.md §3.9). */
export function createProgram(accessToken: string, slug: string): Promise<ProgramAdmin> {
  return apiAdminPost<ProgramAdmin>("/api/programs", accessToken, { slug });
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
