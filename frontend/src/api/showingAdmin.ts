import { apiAdminGet, apiAdminPost, apiAdminPut } from "./adminHttp";
import type { SalesStatus, SpokenLanguage } from "./showings";

export type ShowingAdmin = {
  id: number;
  productionId: number;
  productionSlug: string;
  productionTitle: string | null;
  status: "DRAFT" | "PUBLISHED";
  startsAt: string;
  durationMinutes: number;
  venueName: string;
  venueAddress: string | null;
  spokenLanguage: SpokenLanguage;
  interpretationAvailable: boolean;
  salesStatus: SalesStatus;
  bookingUrl: string | null;
};

export type ShowingDetailsInput = {
  startsAt: string;
  durationMinutes: number;
  venueName: string;
  venueAddress: string | null;
  spokenLanguage: SpokenLanguage;
  interpretationAvailable: boolean;
};

export function listShowingsForAdmin(accessToken: string): Promise<ShowingAdmin[]> {
  return apiAdminGet<ShowingAdmin[]>("/api/showings/manage", accessToken);
}

export function createShowing(
  accessToken: string,
  productionId: number,
  details: ShowingDetailsInput,
): Promise<ShowingAdmin> {
  return apiAdminPost<ShowingAdmin>("/api/showings", accessToken, { productionId, ...details });
}

export function updateShowingDetails(
  accessToken: string,
  id: number,
  details: ShowingDetailsInput,
): Promise<ShowingAdmin> {
  return apiAdminPut<ShowingAdmin>(`/api/showings/${id}`, accessToken, details);
}

/** 1클릭 토글 — PIN을 요구하지 않는다(CLAUDE.md §3.4에 없음). */
export function changeSalesStatus(accessToken: string, id: number, salesStatus: SalesStatus): Promise<ShowingAdmin> {
  return apiAdminPost<ShowingAdmin>(`/api/showings/${id}/sales-status`, accessToken, { salesStatus });
}

/** 파괴적·공개적 동작 — PIN sudo 모드가 열려 있어야 한다(§3.4). */
export function changeBookingUrl(accessToken: string, id: number, bookingUrl: string): Promise<ShowingAdmin> {
  return apiAdminPost<ShowingAdmin>(`/api/showings/${id}/booking-url`, accessToken, { bookingUrl });
}

export function publishShowing(accessToken: string, id: number): Promise<ShowingAdmin> {
  return apiAdminPost<ShowingAdmin>(`/api/showings/${id}/publish`, accessToken);
}

export function unpublishShowing(accessToken: string, id: number): Promise<ShowingAdmin> {
  return apiAdminPost<ShowingAdmin>(`/api/showings/${id}/unpublish`, accessToken);
}
