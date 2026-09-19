package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.media.dto.MediaAssetResponse;
import java.util.List;

public record ArtistAdminResponse(
        Long id,
        String slug,
        String status,
        String linkUrl,
        String email,
        int displayOrder,
        String interviewUrl,
        List<TranslationView> translations,
        List<MediaAssetResponse> images) {

    public record TranslationView(
            String locale,
            String name,
            String role,
            String bio,
            String credits,
            String quote,
            String draftName,
            String draftRole,
            String draftBio,
            String draftCredits,
            String draftQuote,
            boolean hasPendingDraft) {}

    public static ArtistAdminResponse from(Artist artist, List<MediaAssetResponse> images) {
        List<TranslationView> views =
                artist.getTranslations().stream()
                        .map(
                                t ->
                                        new TranslationView(
                                                t.getLocale().name(),
                                                t.getName(),
                                                t.getRole(),
                                                t.getBio(),
                                                t.getCredits(),
                                                t.getQuote(),
                                                t.getDraftName(),
                                                t.getDraftRole(),
                                                t.getDraftBio(),
                                                t.getDraftCredits(),
                                                t.getDraftQuote(),
                                                t.getDraftName() != null))
                        .toList();
        return new ArtistAdminResponse(
                artist.getId(),
                artist.getSlug(),
                artist.getStatus().name(),
                artist.getLinkUrl(),
                artist.getEmail(),
                artist.getDisplayOrder(),
                artist.getInterviewUrl(),
                views,
                images);
    }
}
