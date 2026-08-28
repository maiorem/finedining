import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { queryKeys } from "./queryKeys";

export type ReviewSummary = {
  id: number;
  title: string;
  accountId: number;
  createdAt: string;
};

export type ReviewCommentView = {
  id: number;
  accountId: number;
  body: string;
  createdAt: string;
};

export type ReviewDetail = {
  id: number;
  title: string;
  body: string;
  accountId: number;
  createdAt: string;
  comments: ReviewCommentView[];
};

export function useReviews() {
  return useQuery({
    queryKey: queryKeys.reviews.list,
    queryFn: () => apiGet<ReviewSummary[]>("/api/reviews"),
    staleTime: 5 * 60 * 1000,
  });
}

export function useReview(id: number) {
  return useQuery({
    queryKey: queryKeys.reviews.detail(id),
    queryFn: () => apiGet<ReviewDetail>(`/api/reviews/${id}`),
    staleTime: 5 * 60 * 1000,
    enabled: Number.isFinite(id),
  });
}
