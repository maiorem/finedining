package com.finediningtheater.artist;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import org.junit.jupiter.api.Test;

class CastingTest {

    @Test
    void draft만_있고_공개본_제목이_없으면_공개_조회에서_안_보인다() {
        Casting casting = new Casting();
        CastingTranslation ko = casting.addTranslation(SiteLocale.KO, null, null);
        ko.updateDraft("배우 모집", "지원은 이메일로");

        assertThat(casting.translationFor(SiteLocale.KO)).isNull();
    }

    @Test
    void promoteAllDrafts는_draft를_공개본으로_승격한다() {
        Casting casting = new Casting();
        CastingTranslation ko = casting.addTranslation(SiteLocale.KO, null, null);
        ko.updateDraft("배우 모집", "지원은 이메일로");

        casting.promoteAllDrafts();

        assertThat(casting.translationFor(SiteLocale.KO).getTitle()).isEqualTo("배우 모집");
    }
}
