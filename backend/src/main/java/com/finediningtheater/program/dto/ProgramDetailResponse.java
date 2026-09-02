package com.finediningtheater.program.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.program.Program;
import com.finediningtheater.program.ProgramTranslation;
import java.util.List;

public record ProgramDetailResponse(
        Long id,
        String slug,
        String title,
        String description,
        String applyUrl,
        String locationUrl,
        List<MediaAssetResponse> images) {

    public static ProgramDetailResponse from(
            Program program, SiteLocale locale, List<MediaAssetResponse> images) {
        ProgramTranslation translation = program.translationFor(locale);
        return new ProgramDetailResponse(
                program.getId(),
                program.getSlug(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getDescription(),
                program.getApplyUrl(),
                program.getLocationUrl(),
                images);
    }
}
