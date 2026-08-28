import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { apiAdminGet } from "./adminHttp";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type About = {
  intro: string | null;
};

/**
 * previewAccessToken이 있으면 관리자 미리보기(?preview=true, CLAUDE.md §3.9)로 조회한다 —
 * 아직 한 번도 발행 안 된 소개문도 관리자는 같은 페이지에서 편집 패널에 붙을 수 있어야 한다.
 */
export function useAbout(i18nLanguage: string, previewAccessToken?: string) {
  const lang = toApiLocale(i18nLanguage);
  const preview = Boolean(previewAccessToken);
  return useQuery({
    queryKey: preview ? [...queryKeys.about.detail(lang), "preview"] : queryKeys.about.detail(lang),
    queryFn: () =>
      preview
        ? apiAdminGet<About>(`/api/about?lang=${lang}&preview=true`, previewAccessToken!)
        : apiGet<About>(`/api/about?lang=${lang}`),
    staleTime: preview ? 0 : 60 * 60 * 1000,
  });
}
