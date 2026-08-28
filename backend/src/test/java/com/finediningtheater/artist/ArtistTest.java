package com.finediningtheater.artist;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import org.junit.jupiter.api.Test;

class ArtistTest {

    @Test
    void 요청한_로케일_이름이_있으면_그대로_반환한다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개", "참여작품");
        artist.addTranslation(SiteLocale.EN, "Kim Artist", "Director", "Bio", "Credits");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("Kim Artist");
    }

    @Test
    void 요청한_로케일_이름이_없으면_한국어로_폴백한다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개", "참여작품");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("김아무개");
    }

    @Test
    void draft만_있고_공개본_이름이_없으면_공개_조회에서_안_보인다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개", "참여작품");
        ArtistTranslation en = artist.addTranslation(SiteLocale.EN, null, null, null, null);
        en.updateDraft("Kim Artist (draft)", "Director", "Bio", "Credits");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("김아무개");
    }

    @Test
    void promoteAllDrafts는_draft가_있는_로케일만_공개본으로_승격한다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation ko = artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개", "참여작품");
        ko.updateDraft("새 이름", "새 역할", "새 소개", "새 참여작품");

        artist.promoteAllDrafts();

        assertThat(artist.nameFor(SiteLocale.KO)).isEqualTo("새 이름");
    }
}
