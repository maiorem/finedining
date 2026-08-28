import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

export type ArtistTranslationView = {
  locale: "KO" | "EN";
  name: string | null;
  role: string | null;
  bio: string | null;
  credits: string | null;
  draftName: string | null;
  draftRole: string | null;
  draftBio: string | null;
  draftCredits: string | null;
  hasPendingDraft: boolean;
};

export type ArtistAdmin = {
  id: number;
  slug: string;
  status: "DRAFT" | "PUBLISHED";
  linkUrl: string | null;
  translations: ArtistTranslationView[];
  images: MediaAsset[];
};

/** 목록에서 "새 아티스트 추가"로 호출한다. 슬러그만 받아 DRAFT로 만든다(CLAUDE.md §3.9). */
export function createArtist(accessToken: string, slug: string): Promise<ArtistAdmin> {
  return apiAdminPost<ArtistAdmin>("/api/artists", accessToken, { slug });
}

export function getArtistForAdmin(accessToken: string, id: number): Promise<ArtistAdmin> {
  return apiAdminGet<ArtistAdmin>(`/api/artists/manage/${id}`, accessToken);
}

/** "임시저장" — 공개본에는 영향이 없다(CLAUDE.md §3.9). */
export function saveArtistDraftTranslation(
  accessToken: string,
  id: number,
  locale: "KO" | "EN",
  name: string,
  role: string | null,
  bio: string | null,
  credits: string | null,
): Promise<ArtistAdmin> {
  return apiAdminPut<ArtistAdmin>(`/api/artists/${id}/translations/${locale}`, accessToken, {
    name,
    role,
    bio,
    credits,
  });
}

export function changeArtistLinkUrl(accessToken: string, id: number, linkUrl: string | null): Promise<ArtistAdmin> {
  return apiAdminPut<ArtistAdmin>(`/api/artists/${id}/link`, accessToken, { linkUrl });
}

export function publishArtist(accessToken: string, id: number): Promise<ArtistAdmin> {
  return apiAdminPost<ArtistAdmin>(`/api/artists/${id}/publish`, accessToken);
}

export function unpublishArtist(accessToken: string, id: number): Promise<ArtistAdmin> {
  return apiAdminPost<ArtistAdmin>(`/api/artists/${id}/unpublish`, accessToken);
}
