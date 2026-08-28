package com.finediningtheater.showing.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.showing.Showing;
import java.time.Instant;

public record ShowingAdminResponse(
        Long id,
        Long productionId,
        String productionSlug,
        String productionTitle,
        String status,
        Instant startsAt,
        int durationMinutes,
        String venueName,
        String venueAddress,
        String spokenLanguage,
        boolean interpretationAvailable,
        String salesStatus,
        String bookingUrl) {

    public static ShowingAdminResponse from(Showing showing) {
        Production production = showing.getProduction();
        return new ShowingAdminResponse(
                showing.getId(),
                production.getId(),
                production.getSlug(),
                production.titleFor(SiteLocale.KO),
                showing.getStatus().name(),
                showing.getStartsAt(),
                showing.getDurationMinutes(),
                showing.getVenueName(),
                showing.getVenueAddress(),
                showing.getSpokenLanguage().name(),
                showing.isInterpretationAvailable(),
                showing.getSalesStatus().name(),
                showing.getBookingUrl());
    }
}
