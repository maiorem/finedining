package com.finediningtheater.artist;

import com.finediningtheater.global.support.Publishable;
import com.finediningtheater.global.support.SiteLocale;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * 모집 공고(기능6). 열람 전용이다 — 지원 접수는 범위 밖이고, 지원 방법은 본문에 외부
 * 이메일·폼 링크로 안내한다(CLAUDE.md §3.8). 슬러그가 없다 — 개별 상세 페이지 없이 목록으로만 노출된다.
 */
@Entity
@Getter
@Table(name = "casting")
public class Casting extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "casting", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CastingTranslation> translations = new ArrayList<>();

    public CastingTranslation translationRowFor(SiteLocale locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst().orElse(null);
    }

    public CastingTranslation addTranslation(SiteLocale locale, String title, String body) {
        CastingTranslation translation = new CastingTranslation(this, locale, title, body);
        translations.add(translation);
        return translation;
    }

    /** 공개 조회 전용 — 공개본 제목이 없는 로케일은 한국어로 폴백한다(§7.6). */
    public CastingTranslation translationFor(SiteLocale locale) {
        return translations.stream()
                .filter(t -> t.getLocale() == locale && t.getTitle() != null)
                .findFirst()
                .or(
                        () ->
                                translations.stream()
                                        .filter(t -> t.getLocale() == SiteLocale.KO && t.getTitle() != null)
                                        .findFirst())
                .orElse(null);
    }

    public void promoteAllDrafts() {
        translations.forEach(CastingTranslation::promoteDraftToPublished);
    }
}
