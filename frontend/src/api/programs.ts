import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { apiAdminGet } from "./adminHttp";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";
import type { MediaAsset } from "./media";

export type ProgramSummary = {
  id: number;
  slug: string;
  title: string | null;
  description: string | null;
  applyUrl: string | null;
  locationUrl: string | null;
  thumbnail: MediaAsset | null;
};

export type ProgramDetail = {
  id: number;
  slug: string;
  title: string | null;
  description: string | null;
  applyUrl: string | null;
  locationUrl: string | null;
  images: MediaAsset[];
};

// 네이버 예약처럼 캘린더를 자체 구축하지 않는다 — 이벤트 공지 + 외부 링크(구글폼)로만 받는다(CLAUDE.md §4).
export function usePrograms(i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.programs.list(lang),
    queryFn: () => apiGet<ProgramSummary[]>(`/api/programs?lang=${lang}`),
    staleTime: 60 * 60 * 1000,
  });
}

/**
 * previewAccessToken이 있으면 관리자 미리보기(?preview=true, CLAUDE.md §3.9)로 조회한다 —
 * 아직 발행되지 않은 방금 만든 프로그램도 관리자는 같은 URL에서 편집 패널에 붙을 수 있어야 한다.
 */
export function useProgram(slug: string, i18nLanguage: string, previewAccessToken?: string) {
  const lang = toApiLocale(i18nLanguage);
  const preview = Boolean(previewAccessToken);
  return useQuery({
    queryKey: preview ? [...queryKeys.programs.detail(slug, lang), "preview"] : queryKeys.programs.detail(slug, lang),
    queryFn: () =>
      preview
        ? apiAdminGet<ProgramDetail>(`/api/programs/${slug}?lang=${lang}&preview=true`, previewAccessToken!)
        : apiGet<ProgramDetail>(`/api/programs/${slug}?lang=${lang}`),
    staleTime: preview ? 0 : 60 * 60 * 1000,
    enabled: slug.length > 0,
  });
}
