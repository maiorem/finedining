package com.finediningtheater.program.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.program.Program;
import com.finediningtheater.program.ProgramTranslation;

public record ProgramResponse(
        Long id,
        String slug,
        String title,
        String subtitle,
        String applyUrl,
        String locationUrl,
        MediaAssetResponse thumbnail) {

    public static ProgramResponse from(Program program, SiteLocale locale, MediaAssetResponse thumbnail) {
        ProgramTranslation translation = program.translationFor(locale);
        return new ProgramResponse(
                program.getId(),
                program.getSlug(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getSubtitle(),
                program.getApplyUrl(),
                program.getLocationUrl(),
                thumbnail);
    }
}
