package com.finediningtheater.artist.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;

/** 아티스트 응답에 곁들이는 참여 작품 요약. */
public record ProductionRef(Long id, String slug, String title) {

    public static ProductionRef from(Production production, SiteLocale locale) {
        return new ProductionRef(production.getId(), production.getSlug(), production.titleFor(locale));
    }
}
