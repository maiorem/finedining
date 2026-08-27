import { useQuery } from "@tanstack/react-query";
import { apiGet } from "./http";
import { toApiLocale } from "./locale";
import { queryKeys } from "./queryKeys";

export type SalesStatus = "OPEN" | "CLOSING_SOON" | "SOLD_OUT" | "ENDED";
export type SpokenLanguage = "KO" | "EN";

export type Showing = {
  id: number;
  productionSlug: string;
  productionTitle: string;
  startsAt: string;
  durationMinutes: number;
  venueName: string;
  venueAddress: string | null;
  spokenLanguage: SpokenLanguage;
  interpretationAvailable: boolean;
  salesStatus: SalesStatus;
  bookingUrl: string | null;
  bookingAvailable: boolean;
};

export type ShowingListParams = {
  productionSlug?: string;
  from?: string; // yyyy-MM-dd
  to?: string; // yyyy-MM-dd
  lang: string; // i18next 언어 코드
};

export function useShowings(params: ShowingListParams) {
  const lang = toApiLocale(params.lang);
  const search = new URLSearchParams();
  if (params.productionSlug) search.set("productionSlug", params.productionSlug);
  if (params.from) search.set("from", params.from);
  if (params.to) search.set("to", params.to);
  search.set("lang", lang);

  return useQuery({
    queryKey: queryKeys.showings.list({ ...params, lang }),
    queryFn: () => apiGet<Showing[]>(`/api/showings?${search.toString()}`),
    // 회차 목록은 5분 (CLAUDE.md §9).
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * 예약 링크 클릭 이탈 트래킹 (CLAUDE.md §4). 응답을 기다리지 않고 즉시 이동해야 하므로
 * sendBeacon으로 발사만 하고 끝낸다 — 트래킹 실패가 예약을 막아서는 안 된다.
 */
export function trackBookingClick(showingId: number, channel: string, locale: string) {
  if (typeof navigator === "undefined" || !navigator.sendBeacon) return;

  const pageParams = new URLSearchParams(window.location.search);
  const payload = JSON.stringify({
    channel,
    locale,
    utmSource: pageParams.get("utm_source"),
    utmMedium: pageParams.get("utm_medium"),
    utmCampaign: pageParams.get("utm_campaign"),
  });

  navigator.sendBeacon(
    `/api/showings/${showingId}/booking-click`,
    new Blob([payload], { type: "application/json" }),
  );
}
