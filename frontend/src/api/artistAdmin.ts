import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

export type ArtistTranslationView = {
  locale: "KO" | "EN";
  name: string | null;
  role: string | null;
  bio: string | null;
  draftName: string | null;
  draftRole: string | null;
  draftBio: string | null;
  hasPendingDraft: boolean;
};

export type ArtistProductionRef = {
  id: number;
  slug: string;
  title: string | null;
};

export type ArtistAdmin = {
  id: number;
  slug: string;
  status: "DRAFT" | "PUBLISHED";
  linkUrl: string | null;
  translations: ArtistTranslationView[];
  productions: ArtistProductionRef[];
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
): Promise<ArtistAdmin> {
  return apiAdminPut<ArtistAdmin>(`/api/artists/${id}/translations/${locale}`, accessToken, { name, role, bio });
}

export function changeArtistLinkUrl(accessToken: string, id: number, linkUrl: string | null): Promise<ArtistAdmin> {
  return apiAdminPut<ArtistAdmin>(`/api/artists/${id}/link`, accessToken, { linkUrl });
}

/** 참여작품 전체를 이 목록으로 치환한다(추가/삭제가 아니라 교체) — 백엔드도 같은 의미다. */
export function updateArtistProductions(accessToken: string, id: number, productionIds: number[]): Promise<ArtistAdmin> {
  return apiAdminPut<ArtistAdmin>(`/api/artists/${id}/productions`, accessToken, { productionIds });
}

export function publishArtist(accessToken: string, id: number): Promise<ArtistAdmin> {
  return apiAdminPost<ArtistAdmin>(`/api/artists/${id}/publish`, accessToken);
}

export function unpublishArtist(accessToken: string, id: number): Promise<ArtistAdmin> {
  return apiAdminPost<ArtistAdmin>(`/api/artists/${id}/unpublish`, accessToken);
}
