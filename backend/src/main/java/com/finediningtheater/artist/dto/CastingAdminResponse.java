package com.finediningtheater.artist.dto;

import com.finediningtheater.artist.Casting;
import java.util.List;

public record CastingAdminResponse(Long id, String status, List<TranslationView> translations) {

    public record TranslationView(
            String locale, String title, String body, String draftTitle, String draftBody, boolean hasPendingDraft) {}

    public static CastingAdminResponse from(Casting casting) {
        List<TranslationView> views =
                casting.getTranslations().stream()
                        .map(
                                t ->
                                        new TranslationView(
                                                t.getLocale().name(),
                                                t.getTitle(),
                                                t.getBody(),
                                                t.getDraftTitle(),
                                                t.getDraftBody(),
                                                t.getDraftTitle() != null))
                        .toList();
        return new CastingAdminResponse(casting.getId(), casting.getStatus().name(), views);
    }
}
