package com.finediningtheater.artist;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import org.junit.jupiter.api.Test;

class ArtistTranslationTest {

    @Test
    void draft가_없으면_effective값은_공개본이다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation translation = artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");

        assertThat(translation.effectiveName()).isEqualTo("김아무개");
        assertThat(translation.effectiveRole()).isEqualTo("연출");
        assertThat(translation.effectiveBio()).isEqualTo("소개");
    }

    @Test
    void draft가_있으면_effective값은_draft다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation translation = artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");

        translation.updateDraft("새 이름", "새 역할", "새 소개");

        assertThat(translation.effectiveName()).isEqualTo("새 이름");
        assertThat(translation.getName()).isEqualTo("김아무개");
    }

    @Test
    void promoteDraftToPublished는_draft를_공개본으로_복사한다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation translation = artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        translation.updateDraft("새 이름", "새 역할", "새 소개");

        translation.promoteDraftToPublished();

        assertThat(translation.getName()).isEqualTo("새 이름");
        assertThat(translation.getRole()).isEqualTo("새 역할");
        assertThat(translation.getBio()).isEqualTo("새 소개");
    }
}
