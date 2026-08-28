import { apiAdminDelete, apiAdminPost, apiAdminPut } from "./adminHttp";

export type MediaOwnerType = "PRODUCTION" | "ARTIST";

// 뷰어 무관 데이터라 공개·관리자 응답 모두 이 모양을 공유한다 (CLAUDE.md §7.2 — 백엔드
// MediaAssetResponse와 1:1로 대응).
export type MediaAsset = {
  id: number;
  status: "PENDING" | "READY" | "FAILED";
  failureReason: string | null;
  width: number | null;
  height: number | null;
  altText: string | null;
  lqipBase64: string | null;
  url640: string | null;
  url960: string | null;
  url1600: string | null;
  published: boolean;
};

type PresignResult = { mediaAssetId: number; uploadUrl: string };

export function presignUpload(
  accessToken: string,
  ownerType: MediaOwnerType,
  ownerId: number,
  file: File,
): Promise<PresignResult> {
  return apiAdminPost<PresignResult>("/api/media/presign", accessToken, {
    ownerType,
    ownerId,
    contentType: file.type,
    contentLengthBytes: file.size,
  });
}

/** 브라우저 → S3/MinIO 직접 업로드. 우리 서버는 이 바이트를 통과시키지 않는다 (CLAUDE.md §7.5). */
export async function uploadToPresignedUrl(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": file.type },
    body: file,
  });
  if (!response.ok) {
    throw new Error("이미지 업로드에 실패했습니다.");
  }
}

export function completeMediaUpload(
  accessToken: string,
  mediaAssetId: number,
  altText: string,
): Promise<MediaAsset> {
  return apiAdminPost<MediaAsset>(`/api/media/${mediaAssetId}/complete`, accessToken, { altText });
}

export function deleteMedia(accessToken: string, mediaAssetId: number): Promise<void> {
  return apiAdminDelete<void>(`/api/media/${mediaAssetId}`, accessToken);
}

/** 이미 업로드가 끝난 이미지의 캡션(대체 텍스트)만 고친다 — 다시 올리지 않아도 된다. */
export function updateMediaAltText(accessToken: string, mediaAssetId: number, altText: string): Promise<MediaAsset> {
  return apiAdminPut<MediaAsset>(`/api/media/${mediaAssetId}`, accessToken, { altText });
}
