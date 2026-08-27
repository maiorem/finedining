package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import org.junit.jupiter.api.Test;

class ProductionTest {

    @Test
    void 요청한_로케일_번역이_있으면_그대로_반환한다() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        production.addTranslation(SiteLocale.EN, "Showcase", null);

        assertThat(production.titleFor(SiteLocale.EN)).isEqualTo("Showcase");
    }

    @Test
    void 요청한_로케일_번역이_없으면_한국어로_폴백한다() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);

        assertThat(production.titleFor(SiteLocale.EN)).isEqualTo("쇼케이스");
    }

    @Test
    void 어떤_번역도_없으면_null을_반환한다() {
        Production production = new Production("showcase");

        assertThat(production.titleFor(SiteLocale.KO)).isNull();
    }

    @Test
    void draft만_있고_공개본_제목이_없는_로케일은_공개_조회에서_안_보인다() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        ProductionTranslation en = production.addTranslation(SiteLocale.EN, null, null);
        en.updateDraft("Showcase (draft)", null);

        // EN 공개본이 없으니 한국어로 폴백해야 한다 — draft가 새어 나가면 안 된다.
        assertThat(production.titleFor(SiteLocale.EN)).isEqualTo("쇼케이스");
    }

    @Test
    void translationRowFor는_공개_여부와_무관하게_정확히_그_로케일_행을_찾는다() {
        Production production = new Production("showcase");
        ProductionTranslation en = production.addTranslation(SiteLocale.EN, null, null);
        en.updateDraft("Showcase (draft)", null);

        assertThat(production.translationRowFor(SiteLocale.EN)).isSameAs(en);
        assertThat(production.translationRowFor(SiteLocale.KO)).isNull();
    }

    @Test
    void promoteAllDrafts는_draft가_있는_로케일만_공개본으로_승격한다() {
        Production production = new Production("showcase");
        ProductionTranslation ko = production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        ko.updateDraft("새 제목", "새 부제");
        production.addTranslation(SiteLocale.EN, "Showcase", "Old subtitle"); // draft 없음

        production.promoteAllDrafts();

        assertThat(production.titleFor(SiteLocale.KO)).isEqualTo("새 제목");
        assertThat(production.titleFor(SiteLocale.EN)).isEqualTo("Showcase");
    }
}
