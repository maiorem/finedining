import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type ArtistSummary = {
  id: number;
  slug: string;
  name: string | null;
  role: string | null;
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

export function useArtist(slug: string, i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.artists.detail(slug, lang),
    queryFn: () => apiGet<ArtistDetail>(`/api/artists/${slug}?lang=${lang}`),
    staleTime: 60 * 60 * 1000,
    enabled: slug.length > 0,
  });
}
