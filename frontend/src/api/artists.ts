import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { apiAdminGet } from "./adminHttp";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";
import type { MediaAsset } from "./media";

export type ArtistSummary = {
  id: number;
  slug: string;
  name: string | null;
  role: string | null;
  photo: MediaAsset | null;
};

export type ArtistProductionRef = {
  id: number;
  slug: string;
  title: string | null;
};

export type ArtistDetail = {
  id: number;
  slug: string;
  name: string | null;
  role: string | null;
  bio: string | null;
  linkUrl: string | null;
  photo: MediaAsset | null;
  productions: ArtistProductionRef[];
};

export function useArtists(i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.artists.list(lang),
    queryFn: () => apiGet<ArtistSummary[]>(`/api/artists?lang=${lang}`),
    // 아티스트 프로필도 거의 정적이다 (CLAUDE.md §9).
    staleTime: 60 * 60 * 1000,
  });
}

/**
 * previewAccessToken이 있으면 관리자 미리보기(?preview=true, CLAUDE.md §3.9)로 조회한다 —
 * 아직 발행되지 않은 방금 만든 아티스트도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
 */
export function useArtist(slug: string, i18nLanguage: string, previewAccessToken?: string) {
  const lang = toApiLocale(i18nLanguage);
  const preview = Boolean(previewAccessToken);
  return useQuery({
    queryKey: preview ? [...queryKeys.artists.detail(slug, lang), "preview"] : queryKeys.artists.detail(slug, lang),
    queryFn: () =>
      preview
        ? apiAdminGet<ArtistDetail>(`/api/artists/${slug}?lang=${lang}&preview=true`, previewAccessToken!)
        : apiGet<ArtistDetail>(`/api/artists/${slug}?lang=${lang}`),
    staleTime: preview ? 0 : 60 * 60 * 1000,
    enabled: slug.length > 0,
  });
}
