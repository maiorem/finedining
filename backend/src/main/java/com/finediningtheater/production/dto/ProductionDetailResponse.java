package com.finediningtheater.production.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionTranslation;

public record ProductionDetailResponse(Long id, String slug, String title, String subtitle) {

    public static ProductionDetailResponse from(Production production, SiteLocale locale) {
        ProductionTranslation translation = production.translationFor(locale);
        return new ProductionDetailResponse(
                production.getId(),
                production.getSlug(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getSubtitle());
    }
}
