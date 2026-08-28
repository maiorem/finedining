import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { apiAdminGet } from "./adminHttp";
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

/**
 * previewAccessToken이 있으면 관리자 미리보기(?preview=true, CLAUDE.md §3.9)로 조회한다 —
 * 아직 발행되지 않은 방금 만든 작품도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
 * 미리보기는 방금 저장한 값이 바로 보여야 하므로 staleTime을 두지 않는다(§9).
 */
export function useProduction(slug: string, i18nLanguage: string, previewAccessToken?: string) {
  const lang = toApiLocale(i18nLanguage);
  const preview = Boolean(previewAccessToken);
  return useQuery({
    queryKey: preview ? [...queryKeys.productions.detail(slug, lang), "preview"] : queryKeys.productions.detail(slug, lang),
    queryFn: () =>
      preview
        ? apiAdminGet<ProductionDetail>(`/api/productions/${slug}?lang=${lang}&preview=true`, previewAccessToken!)
        : apiGet<ProductionDetail>(`/api/productions/${slug}?lang=${lang}`),
    staleTime: preview ? 0 : 60 * 60 * 1000,
    enabled: slug.length > 0,
  });
}
