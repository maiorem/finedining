package com.finediningtheater.about.dto;

import com.finediningtheater.about.AboutContent;
import java.util.List;

public record AboutAdminResponse(Long id, String status, List<TranslationView> translations) {

    public record TranslationView(String locale, String intro, String draftIntro, boolean hasPendingDraft) {}

    public static AboutAdminResponse from(AboutContent about) {
        List<TranslationView> views =
                about.getTranslations().stream()
                        .map(
                                t ->
                                        new TranslationView(
                                                t.getLocale().name(),
                                                t.getIntro(),
                                                t.getDraftIntro(),
                                                t.getDraftIntro() != null))
                        .toList();
        return new AboutAdminResponse(about.getId(), about.getStatus().name(), views);
    }
}
