package com.finediningtheater.production.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionTranslation;
import java.util.List;

public record ProductionDetailResponse(
        Long id, String slug, String title, String subtitle, String description, List<MediaAssetResponse> images) {

    public static ProductionDetailResponse from(
            Production production, SiteLocale locale, List<MediaAssetResponse> images) {
        ProductionTranslation translation = production.translationFor(locale);
        return new ProductionDetailResponse(
                production.getId(),
                production.getSlug(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getSubtitle(),
                translation == null ? null : translation.getDescription(),
                images);
    }
}
