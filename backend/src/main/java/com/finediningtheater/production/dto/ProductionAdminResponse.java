package com.finediningtheater.production.dto;

import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.production.Production;
import java.util.List;

public record ProductionAdminResponse(
        Long id,
        String slug,
        String status,
        String bookingUrl,
        String locationUrl,
        List<TranslationView> translations,
        List<MediaAssetResponse> images) {

    public record TranslationView(
            String locale,
            String title,
            String subtitle,
            String description,
            String draftTitle,
            String draftSubtitle,
            String draftDescription,
            boolean hasPendingDraft) {}

    public static ProductionAdminResponse from(Production production, List<MediaAssetResponse> images) {
        List<TranslationView> views =
                production.getTranslations().stream()
                        .map(
                                t ->
                                        new TranslationView(
                                                t.getLocale().name(),
                                                t.getTitle(),
                                                t.getSubtitle(),
                                                t.getDescription(),
                                                t.getDraftTitle(),
                                                t.getDraftSubtitle(),
                                                t.getDraftDescription(),
                                                t.getDraftTitle() != null))
                        .toList();
        return new ProductionAdminResponse(
                production.getId(),
                production.getSlug(),
                production.getStatus().name(),
                production.getBookingUrl(),
                production.getLocationUrl(),
                views,
                images);
    }
}
