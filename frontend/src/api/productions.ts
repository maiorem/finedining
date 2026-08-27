import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type ProductionSummary = {
  id: number;
  slug: string;
  title: string;
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
