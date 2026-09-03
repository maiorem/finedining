import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { MediaAsset } from "./media";

export type PressClippingAdmin = {
  id: number;
  title: string;
  externalUrl: string;
  status: "DRAFT" | "PUBLISHED";
  images: MediaAsset[];
};

export function listPressClippingsForAdmin(accessToken: string): Promise<PressClippingAdmin[]> {
  return apiAdminGet<PressClippingAdmin[]>("/api/press-clippings/manage", accessToken);
}

export function getPressClippingForAdmin(accessToken: string, id: number): Promise<PressClippingAdmin> {
  return apiAdminGet<PressClippingAdmin>(`/api/press-clippings/manage/${id}`, accessToken);
}

export function createPressClipping(
  accessToken: string,
  title: string,
  externalUrl: string,
): Promise<PressClippingAdmin> {
  return apiAdminPost<PressClippingAdmin>("/api/press-clippings", accessToken, { title, externalUrl });
}

/** 제목·링크는 draft 없이 즉시 반영된다(CLAUDE.md §3.9, Artist.linkUrl과 같은 취급). */
export function updatePressClippingContent(
  accessToken: string,
  id: number,
  title: string,
  externalUrl: string,
): Promise<PressClippingAdmin> {
  return apiAdminPut<PressClippingAdmin>(`/api/press-clippings/${id}`, accessToken, { title, externalUrl });
}

export function publishPressClipping(accessToken: string, id: number): Promise<PressClippingAdmin> {
  return apiAdminPost<PressClippingAdmin>(`/api/press-clippings/${id}/publish`, accessToken);
}

export function unpublishPressClipping(accessToken: string, id: number): Promise<PressClippingAdmin> {
  return apiAdminPost<PressClippingAdmin>(`/api/press-clippings/${id}/unpublish`, accessToken);
}
