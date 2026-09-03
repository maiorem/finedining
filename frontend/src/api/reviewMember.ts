import { apiMemberDelete, apiMemberPost, apiMemberPut } from "./memberHttp";
import type { ReviewCommentView } from "./reviews";

export type ReviewWriteResult = {
  id: number;
  title: string;
  body: string;
  accountId: number;
  status: "PUBLISHED" | "HIDDEN" | "DELETED";
  createdAt: string;
  updatedAt: string;
  comments: ReviewCommentView[];
};

export function createReview(accessToken: string, title: string, body: string): Promise<ReviewWriteResult> {
  return apiMemberPost<ReviewWriteResult>("/api/reviews", accessToken, { title, body });
}

/** 본인 글만 수정할 수 있다 — 남의 글이면 서버가 POST_NOT_OWNED로 거부한다(CLAUDE.md §3.3). */
export function updateOwnReview(
  accessToken: string,
  id: number,
  title: string,
  body: string,
): Promise<ReviewWriteResult> {
  return apiMemberPut<ReviewWriteResult>(`/api/reviews/${id}`, accessToken, { title, body });
}

/** 본인 글만 삭제할 수 있다 — 남의 글이면 서버가 POST_NOT_OWNED로 거부한다(CLAUDE.md §3.3). */
export function deleteOwnReview(accessToken: string, id: number): Promise<ReviewWriteResult> {
  return apiMemberDelete<ReviewWriteResult>(`/api/reviews/${id}`, accessToken);
}
