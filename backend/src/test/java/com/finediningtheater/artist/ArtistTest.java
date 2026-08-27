package com.finediningtheater.artist;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import org.junit.jupiter.api.Test;

class ArtistTest {

    @Test
    void 요청한_로케일_이름이_있으면_그대로_반환한다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        artist.addTranslation(SiteLocale.EN, "Kim Artist", "Director", "Bio");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("Kim Artist");
    }

    @Test
    void 요청한_로케일_이름이_없으면_한국어로_폴백한다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("김아무개");
    }

    @Test
    void draft만_있고_공개본_이름이_없으면_공개_조회에서_안_보인다() {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        ArtistTranslation en = artist.addTranslation(SiteLocale.EN, null, null, null);
        en.updateDraft("Kim Artist (draft)", "Director", "Bio");

        assertThat(artist.nameFor(SiteLocale.EN)).isEqualTo("김아무개");
    }

    @Test
    void promoteAllDrafts는_draft가_있는_로케일만_공개본으로_승격한다() {
        Artist artist = new Artist("kim-artist");
        ArtistTranslation ko = artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        ko.updateDraft("새 이름", "새 역할", "새 소개");

        artist.promoteAllDrafts();

        assertThat(artist.nameFor(SiteLocale.KO)).isEqualTo("새 이름");
    }

    @Test
    void replaceProductions는_기존_목록을_새_목록으로_치환한다() {
        Artist artist = new Artist("kim-artist");
        Production p1 = new Production("show-1");
        Production p2 = new Production("show-2");
        artist.replaceProductions(new java.util.HashSet<>(java.util.List.of(p1)));

        artist.replaceProductions(new java.util.HashSet<>(java.util.List.of(p2)));

        assertThat(artist.getProductions()).containsExactly(p2);
    }
}
