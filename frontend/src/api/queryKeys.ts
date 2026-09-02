/**
 * TanStack Query 쿼리 키를 이 파일 한 곳에 상수로 모은다 (CLAUDE.md §9).
 * 도메인 쿼리 키는 각 기능을 구현할 때 여기에 추가한다.
 */
export const queryKeys = {
  productions: {
    all: ["productions"] as const,
    list: (lang: string) => ["productions", "list", lang] as const,
    detail: (slug: string, lang: string) => ["productions", "detail", slug, lang] as const,
    adminDetail: (id: number) => ["productions", "admin", id] as const,
    adminList: ["productions", "admin", "list"] as const,
  },
  programs: {
    all: ["programs"] as const,
    list: (lang: string) => ["programs", "list", lang] as const,
    detail: (slug: string, lang: string) => ["programs", "detail", slug, lang] as const,
    adminList: ["programs", "admin", "list"] as const,
    adminDetail: (id: number) => ["programs", "admin", id] as const,
  },
  artists: {
    all: ["artists"] as const,
    list: (lang: string) => ["artists", "list", lang] as const,
    detail: (slug: string, lang: string) => ["artists", "detail", slug, lang] as const,
    adminDetail: (id: number) => ["artists", "admin", id] as const,
    adminList: ["artists", "admin", "list"] as const,
  },
  castings: {
    list: (lang: string) => ["castings", "list", lang] as const,
  },
  about: {
    detail: (lang: string) => ["about", "detail", lang] as const,
    admin: ["about", "admin"] as const,
  },
  reviews: {
    all: ["reviews"] as const,
    list: ["reviews", "list"] as const,
    detail: (id: number) => ["reviews", "detail", id] as const,
    adminList: ["reviews", "admin", "list"] as const,
    adminDetail: (id: number) => ["reviews", "admin", "detail", id] as const,
  },
  proposals: {
    adminList: ["proposals", "admin", "list"] as const,
  },
} as const;
