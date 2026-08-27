import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";
import type { MediaAsset } from "./media";

export type ProductionSummary = {
  id: number;
  slug: string;
  title: string;
  thumbnail: MediaAsset | null;
};

export type ProductionDetail = {
  id: number;
  slug: string;
  title: string | null;
  subtitle: string | null;
  images: MediaAsset[];
};

export function useProductions(i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.productions.list(lang),
    queryFn: () => apiGet<ProductionSummary[]>(`/api/productions?lang=${lang}`),
    // 작품 정보는 거의 정적이다 (CLAUDE.md §9).
    staleTime: 60 * 60 * 1000,
  });
}

export function useProduction(slug: string, i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.productions.detail(slug, lang),
    queryFn: () => apiGet<ProductionDetail>(`/api/productions/${slug}?lang=${lang}`),
    staleTime: 60 * 60 * 1000,
    enabled: slug.length > 0,
  });
}
