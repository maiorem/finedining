package com.finediningtheater.showing.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.showing.Showing;
import java.time.Instant;

public record ShowingResponse(
        Long id,
        String productionSlug,
        String productionTitle,
        Instant startsAt,
        int durationMinutes,
        String venueName,
        String venueAddress,
        String spokenLanguage,
        boolean interpretationAvailable,
        String salesStatus,
        String bookingUrl,
        boolean bookingAvailable) {

    public static ShowingResponse from(Showing showing, SiteLocale locale) {
        Production production = showing.getProduction();
        boolean bookingAvailable = showing.isBookingAvailable();
        return new ShowingResponse(
                showing.getId(),
                production.getSlug(),
                production.titleFor(locale),
                showing.getStartsAt(),
                showing.getDurationMinutes(),
                showing.getVenueName(),
                showing.getVenueAddress(),
                showing.getSpokenLanguage().name(),
                showing.isInterpretationAvailable(),
                showing.getSalesStatus().name(),
                // 예약 불가 상태에서는 죽은 링크를 내려보내지 않는다 (CLAUDE.md §4).
                bookingAvailable ? showing.getBookingUrl() : null,
                bookingAvailable);
    }
}
