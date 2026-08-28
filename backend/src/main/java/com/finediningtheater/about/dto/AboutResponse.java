package com.finediningtheater.about.dto;

import com.finediningtheater.about.AboutContent;
import com.finediningtheater.about.AboutTranslation;
import com.finediningtheater.global.support.SiteLocale;

public record AboutResponse(String intro) {

    public static AboutResponse from(AboutContent about, SiteLocale locale) {
        AboutTranslation translation = about.translationFor(locale);
        return new AboutResponse(translation == null ? null : translation.getIntro());
    }
}
