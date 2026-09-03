import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { queryKeys } from "./queryKeys";

export type PressClipping = {
  id: number;
  title: string;
  externalUrl: string;
  imageUrl: string | null;
  imageAlt: string | null;
};

// 열람 전용 — 등록·수정·발행은 관리자만 한다 (CLAUDE.md §3.5).
export function usePressClippings() {
  return useQuery({
    queryKey: queryKeys.pressClippings.list,
    queryFn: () => apiGet<PressClipping[]>("/api/press-clippings"),
    staleTime: 60 * 60 * 1000,
  });
}
