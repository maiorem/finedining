import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type Casting = {
  id: number;
  title: string | null;
  body: string | null;
};

// 열람 전용 — 지원 접수는 범위 밖이다 (CLAUDE.md §3.8).
export function useCastings(i18nLanguage: string) {
  const lang = toApiLocale(i18nLanguage);
  return useQuery({
    queryKey: queryKeys.castings.list(lang),
    queryFn: () => apiGet<Casting[]>(`/api/castings?lang=${lang}`),
    staleTime: 60 * 60 * 1000,
  });
}
