package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import org.junit.jupiter.api.Test;

class ProductionTranslationTest {

    @Test
    void draft가_없으면_effective값은_공개본이다() {
        Production production = new Production("showcase");
        ProductionTranslation translation = production.addTranslation(SiteLocale.KO, "쇼케이스", "부제");

        assertThat(translation.effectiveTitle()).isEqualTo("쇼케이스");
        assertThat(translation.effectiveSubtitle()).isEqualTo("부제");
    }

    @Test
    void draft가_있으면_effective값은_draft다() {
        Production production = new Production("showcase");
        ProductionTranslation translation = production.addTranslation(SiteLocale.KO, "쇼케이스", "부제");

        translation.updateDraft("새 제목", "새 부제");

        assertThat(translation.effectiveTitle()).isEqualTo("새 제목");
        assertThat(translation.effectiveSubtitle()).isEqualTo("새 부제");
        // 임시저장은 공개본을 건드리지 않는다 (CLAUDE.md §3.9).
        assertThat(translation.getTitle()).isEqualTo("쇼케이스");
    }

    @Test
    void promoteDraftToPublished는_draft를_공개본으로_복사한다() {
        Production production = new Production("showcase");
        ProductionTranslation translation = production.addTranslation(SiteLocale.KO, "쇼케이스", "부제");
        translation.updateDraft("새 제목", "새 부제");

        translation.promoteDraftToPublished();

        assertThat(translation.getTitle()).isEqualTo("새 제목");
        assertThat(translation.getSubtitle()).isEqualTo("새 부제");
    }

    @Test
    void draft가_없으면_promoteDraftToPublished는_공개본을_그대로_둔다() {
        Production production = new Production("showcase");
        ProductionTranslation translation = production.addTranslation(SiteLocale.KO, "쇼케이스", "부제");

        translation.promoteDraftToPublished();

        assertThat(translation.getTitle()).isEqualTo("쇼케이스");
        assertThat(translation.getSubtitle()).isEqualTo("부제");
    }
}
