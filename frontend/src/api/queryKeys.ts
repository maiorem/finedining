/**
 * TanStack Query 쿼리 키를 이 파일 한 곳에 상수로 모은다 (CLAUDE.md §9).
 * 도메인 쿼리 키는 각 기능을 구현할 때 여기에 추가한다.
 */
export const queryKeys = {
  productions: {
    list: (lang: string) => ["productions", "list", lang] as const,
    detail: (slug: string, lang: string) => ["productions", "detail", slug, lang] as const,
  },
  showings: {
    list: (params: { productionSlug?: string; from?: string; to?: string; lang: string }) =>
      ["showings", "list", params] as const,
  },
  artists: {
    list: (lang: string) => ["artists", "list", lang] as const,
    detail: (slug: string, lang: string) => ["artists", "detail", slug, lang] as const,
  },
  castings: {
    list: (lang: string) => ["castings", "list", lang] as const,
  },
} as const;
