import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

export type ProductionTranslationView = {
  locale: "KO" | "EN";
  title: string | null;
  subtitle: string | null;
  description: string | null;
  draftTitle: string | null;
  draftSubtitle: string | null;
  draftDescription: string | null;
  hasPendingDraft: boolean;
};

export type ProductionAdmin = {
  id: number;
  slug: string;
  status: "DRAFT" | "PUBLISHED";
  bookingUrl: string | null;
  locationUrl: string | null;
  translations: ProductionTranslationView[];
  images: MediaAsset[];
};

export function getProductionForAdmin(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminGet<ProductionAdmin>(`/api/productions/manage/${id}`, accessToken);
}

/** 회차 생성 폼의 작품 선택 목록 — DRAFT 작품도 포함한다. */
export function listProductionsForAdmin(accessToken: string): Promise<ProductionAdmin[]> {
  return apiAdminGet<ProductionAdmin[]>("/api/productions/manage", accessToken);
}

/** 목록에서 "새 작품 추가"로 호출한다. 슬러그만 받아 DRAFT로 만든다(CLAUDE.md §3.9). */
export function createProduction(accessToken: string, slug: string): Promise<ProductionAdmin> {
  return apiAdminPost<ProductionAdmin>("/api/productions", accessToken, { slug });
}

/** "임시저장" — 공개본에는 영향이 없다(CLAUDE.md §3.9). */
export function saveDraftTranslation(
  accessToken: string,
  id: number,
  locale: "KO" | "EN",
  title: string,
  subtitle: string | null,
  description: string | null,
): Promise<ProductionAdmin> {
  return apiAdminPut<ProductionAdmin>(`/api/productions/${id}/translations/${locale}`, accessToken, {
    title,
    subtitle,
    description,
  });
}

export function publishProduction(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminPost<ProductionAdmin>(`/api/productions/${id}/publish`, accessToken);
}

export function unpublishProduction(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminPost<ProductionAdmin>(`/api/productions/${id}/unpublish`, accessToken);
}

/** 예약 URL 변경 — 파괴적·공개적 동작이라 PIN sudo 모드가 필요하다(CLAUDE.md §3.4). */
export function changeProductionBookingUrl(
  accessToken: string,
  id: number,
  bookingUrl: string | null,
): Promise<ProductionAdmin> {
  return apiAdminPut<ProductionAdmin>(`/api/productions/${id}/booking-url`, accessToken, { bookingUrl });
}

export function changeProductionLocationUrl(
  accessToken: string,
  id: number,
  locationUrl: string | null,
): Promise<ProductionAdmin> {
  return apiAdminPut<ProductionAdmin>(`/api/productions/${id}/location-url`, accessToken, { locationUrl });
}
