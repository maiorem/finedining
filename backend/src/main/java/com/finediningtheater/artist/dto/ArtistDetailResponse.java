package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.artist.ArtistTranslation;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;

public record ArtistDetailResponse(
        Long id,
        String slug,
        String name,
        String role,
        String bio,
        String credits,
        String quote,
        String email,
        String interviewUrl,
        String linkUrl,
        MediaAssetResponse photo) {

    public static ArtistDetailResponse from(Artist artist, SiteLocale locale, MediaAssetResponse photo) {
        ArtistTranslation translation = artist.translationFor(locale);
        return new ArtistDetailResponse(
                artist.getId(),
                artist.getSlug(),
                translation == null ? null : translation.getName(),
                translation == null ? null : translation.getRole(),
                translation == null ? null : translation.getBio(),
                translation == null ? null : translation.getCredits(),
                translation == null ? null : translation.getQuote(),
                artist.getEmail(),
                artist.getInterviewUrl(),
                artist.getLinkUrl(),
                photo);
    }
}
