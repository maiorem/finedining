package com.finediningtheater.production.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;

public record ProductionSummaryResponse(Long id, String slug, String title) {

    public static ProductionSummaryResponse from(Production production, SiteLocale locale) {
        return new ProductionSummaryResponse(
                production.getId(), production.getSlug(), production.titleFor(locale));
    }
}
