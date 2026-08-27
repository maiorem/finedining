package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Casting;
import com.finediningtheater.artist.CastingTranslation;
import com.finediningtheater.global.support.SiteLocale;

public record CastingResponse(Long id, String title, String body) {

    public static CastingResponse from(Casting casting, SiteLocale locale) {
        CastingTranslation translation = casting.translationFor(locale);
        return new CastingResponse(
                casting.getId(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getBody());
    }
}
