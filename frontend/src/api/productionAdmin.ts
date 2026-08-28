import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

export type ProductionTranslationView = {
  locale: "KO" | "EN";
  title: string | null;
  subtitle: string | null;
  draftTitle: string | null;
  draftSubtitle: string | null;
  hasPendingDraft: boolean;
};

export type ProductionAdmin = {
  id: number;
  slug: string;
  status: "DRAFT" | "PUBLISHED";
  translations: ProductionTranslationView[];
  images: MediaAsset[];
};

export function getProductionForAdmin(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminGet<ProductionAdmin>(`/api/productions/manage/${id}`, accessToken);
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
): Promise<ProductionAdmin> {
  return apiAdminPut<ProductionAdmin>(`/api/productions/${id}/translations/${locale}`, accessToken, {
    title,
    subtitle,
  });
}

export function publishProduction(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminPost<ProductionAdmin>(`/api/productions/${id}/publish`, accessToken);
}

export function unpublishProduction(accessToken: string, id: number): Promise<ProductionAdmin> {
  return apiAdminPost<ProductionAdmin>(`/api/productions/${id}/unpublish`, accessToken);
}
