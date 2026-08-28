import { apiAdminDelete, apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { ReviewCommentView } from "./reviews";

export type ReviewAdmin = {
  id: number;
  title: string;
  body: string;
  accountId: number;
  status: "PUBLISHED" | "HIDDEN" | "DELETED";
  createdAt: string;
  updatedAt: string;
  comments: ReviewCommentView[];
};

export function listReviewsForAdmin(accessToken: string): Promise<ReviewAdmin[]> {
  return apiAdminGet<ReviewAdmin[]>("/api/reviews/manage", accessToken);
}

export function getReviewForAdmin(accessToken: string, id: number): Promise<ReviewAdmin> {
  return apiAdminGet<ReviewAdmin>(`/api/reviews/manage/${id}`, accessToken);
}

/** 관리자는 남의 글을 원문 수정까지 할 수 있다 (CLAUDE.md §3.6). */
export function updateReviewContent(
  accessToken: string,
  id: number,
  title: string,
  body: string,
): Promise<ReviewAdmin> {
  return apiAdminPut<ReviewAdmin>(`/api/reviews/${id}`, accessToken, { title, body });
}

export function hideReview(accessToken: string, id: number): Promise<ReviewAdmin> {
  return apiAdminPost<ReviewAdmin>(`/api/reviews/${id}/hide`, accessToken);
}

export function restoreReview(accessToken: string, id: number): Promise<ReviewAdmin> {
  return apiAdminPost<ReviewAdmin>(`/api/reviews/${id}/restore`, accessToken);
}

export function deleteReview(accessToken: string, id: number): Promise<ReviewAdmin> {
  return apiAdminDelete<ReviewAdmin>(`/api/reviews/${id}`, accessToken);
}

export function deleteReviewComment(accessToken: string, commentId: number): Promise<void> {
  return apiAdminDelete<void>(`/api/reviews/comments/${commentId}`, accessToken);
}
