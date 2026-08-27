package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.global.support.SiteLocale;

public record ArtistSummaryResponse(Long id, String slug, String name, String role) {

    public static ArtistSummaryResponse from(Artist artist, SiteLocale locale) {
        var translation = artist.translationFor(locale);
        return new ArtistSummaryResponse(
                artist.getId(),
                artist.getSlug(),
                artist.nameFor(locale),
                translation == null ? null : translation.getRole());
    }
}
