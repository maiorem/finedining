package com.finediningtheater.program.dto;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.program.Program;
import com.finediningtheater.program.ProgramTranslation;

public record ProgramResponse(Long id, String title, String description, String applyUrl, String locationUrl) {

    public static ProgramResponse from(Program program, SiteLocale locale) {
        ProgramTranslation translation = program.translationFor(locale);
        return new ProgramResponse(
                program.getId(),
                translation == null ? null : translation.getTitle(),
                translation == null ? null : translation.getDescription(),
                program.getApplyUrl(),
                program.getLocationUrl());
    }
}
