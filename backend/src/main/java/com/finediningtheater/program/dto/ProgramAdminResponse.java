package com.finediningtheater.program.dto;

import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.program.Program;
import java.util.List;

public record ProgramAdminResponse(
        Long id,
        String slug,
        String status,
        String applyUrl,
        String locationUrl,
        List<TranslationView> translations,
        List<MediaAssetResponse> images) {

    public record TranslationView(
            String locale, String title, String description, String draftTitle, String draftDescription, boolean hasPendingDraft) {}

    public static ProgramAdminResponse from(Program program, List<MediaAssetResponse> images) {
        List<TranslationView> views =
                program.getTranslations().stream()
                        .map(
                                t ->
                                        new TranslationView(
                                                t.getLocale().name(),
                                                t.getTitle(),
                                                t.getDescription(),
                                                t.getDraftTitle(),
                                                t.getDraftDescription(),
                                                t.getDraftTitle() != null))
                        .toList();
        return new ProgramAdminResponse(
                program.getId(),
                program.getSlug(),
                program.getStatus().name(),
                program.getApplyUrl(),
                program.getLocationUrl(),
                views,
                images);
    }
}
