package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Artist;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;
import java.util.List;

public record ArtistAdminResponse(
        Long id,
        String slug,
        String status,
        String linkUrl,
        List<TranslationView> translations,
        List<ProductionRef> productions,
        List<MediaAssetResponse> images) {

    public record TranslationView(
            String locale,
            String name,
            String role,
            String bio,
            String draftName,
            String draftRole,
            String draftBio,
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
                                                t.getDraftName(),
                                                t.getDraftRole(),
                                                t.getDraftBio(),
                                                t.getDraftName() != null))
                        .toList();
        List<ProductionRef> productions =
                artist.getProductions().stream()
                        .map(p -> ProductionRef.from(p, SiteLocale.KO))
                        .toList();
        return new ArtistAdminResponse(
                artist.getId(),
                artist.getSlug(),
                artist.getStatus().name(),
                artist.getLinkUrl(),
                views,
                productions,
                images);
    }
}
