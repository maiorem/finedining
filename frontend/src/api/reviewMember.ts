import { apiMemberDelete, apiMemberPost, apiMemberPut } from "./memberHttp";
import { uploadToPresignedUrl, type MediaAsset } from "./media";
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

export type CreateReviewInput = {
  title: string;
  body: string;
  authorName: string;
  contact: string;
  privacyConsent: boolean;
};

export function createReview(accessToken: string, input: CreateReviewInput): Promise<ReviewWriteResult> {
  return apiMemberPost<ReviewWriteResult>("/api/reviews", accessToken, {
    ...input,
    contact: input.contact.trim() || null,
  });
}

/** 글을 만든 뒤 그 글에 이미지 한 장을 올린다 — presign → S3 직접 PUT → 완료(서버가 소유권·3장·10MB 검사). */
export async function uploadReviewImage(accessToken: string, reviewId: number, file: File): Promise<MediaAsset> {
  const { mediaAssetId, uploadUrl } = await apiMemberPost<{ mediaAssetId: number; uploadUrl: string }>(
    `/api/reviews/${reviewId}/images/presign`,
    accessToken,
    { contentType: file.type, contentLengthBytes: file.size },
  );
  await uploadToPresignedUrl(uploadUrl, file);
  return apiMemberPost<MediaAsset>(`/api/reviews/${reviewId}/images/${mediaAssetId}/complete`, accessToken);
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
