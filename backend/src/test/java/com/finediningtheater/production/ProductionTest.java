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
}
