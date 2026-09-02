import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type Program = {
  id: number;
  title: string | null;
  description: string | null;
  applyUrl: string | null;
  locationUrl: string | null;
};

// 네이버 예약처럼 캘린더를 자체 구축하지 않는다 — 이벤트 공지 + 외부 링크(구글폼)로만 받는다(CLAUDE.md §4).
export function usePrograms(i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.programs.list(lang),
    queryFn: () => apiGet<Program[]>(`/api/programs?lang=${lang}`),
    staleTime: 60 * 60 * 1000,
  });
}
