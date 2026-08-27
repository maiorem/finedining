package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.artist.ArtistTranslation;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;

public record ArtistDetailResponse(
        Long id,
        String slug,
        String name,
        String role,
        String bio,
        String linkUrl,
        List<ProductionRef> productions) {

    public static ArtistDetailResponse from(Artist artist, SiteLocale locale) {
        ArtistTranslation translation = artist.translationFor(locale);
        List<ProductionRef> productions =
                artist.getProductions().stream().map(p -> ProductionRef.from(p, locale)).toList();
        return new ArtistDetailResponse(
                artist.getId(),
                artist.getSlug(),
                translation == null ? null : translation.getName(),
                translation == null ? null : translation.getRole(),
                translation == null ? null : translation.getBio(),
                artist.getLinkUrl(),
                productions);
    }
}
