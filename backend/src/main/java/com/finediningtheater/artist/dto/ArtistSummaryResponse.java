package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;

public record ArtistSummaryResponse(Long id, String slug, String name, String role, MediaAssetResponse photo) {

    public static ArtistSummaryResponse from(Artist artist, SiteLocale locale, MediaAssetResponse photo) {
        var translation = artist.translationFor(locale);
        return new ArtistSummaryResponse(
                artist.getId(),
                artist.getSlug(),
                artist.nameFor(locale),
                translation == null ? null : translation.getRole(),
                photo);
    }
}
