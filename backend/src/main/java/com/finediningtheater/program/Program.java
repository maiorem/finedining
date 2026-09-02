package com.finediningtheater.program;

import com.finediningtheater.global.support.Publishable;
import com.finediningtheater.global.support.SiteLocale;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * 프로그램(이벤트 공지). 네이버 예약 캘린더처럼 회차별 일정을 자체 구축하지 않는 콘텐츠와 같은
 * 이유로, 참가 신청은 구글폼(applyUrl) 외부 링크로 받는다. 슬러그 없이 목록으로만 노출된다 —
 * Casting과 같은 패턴이다.
 */
@Entity
@Getter
@Table(name = "program")
public class Program extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 참가하기 — 구글폼 등 외부 신청 링크. 화이트리스트 검증 없음(Artist.linkUrl과 같은 취급). */
    @Column(length = 500)
    private String applyUrl;

    /** 위치보기 — 행사 장소 링크. */
    @Column(length = 500)
    private String locationUrl;

    @OneToMany(mappedBy = "program", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ProgramTranslation> translations = new HashSet<>();

    protected Program() {}

    /** 공개 조회 전용 — 공개본 제목이 없는(초안뿐인) 로케일은 한국어로 폴백한다(§7.6). */
    public ProgramTranslation translationFor(SiteLocale locale) {
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

    public String titleFor(SiteLocale locale) {
        ProgramTranslation translation = translationFor(locale);
        return translation == null ? null : translation.getTitle();
    }

    /** 편집용. 공개 여부와 무관하게 정확히 그 로케일 행을 찾는다. */
    public ProgramTranslation translationRowFor(SiteLocale locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst().orElse(null);
    }

    public ProgramTranslation addTranslation(SiteLocale locale, String title, String description) {
        ProgramTranslation translation = new ProgramTranslation(this, locale, title, description);
        translations.add(translation);
        return translation;
    }

    public void promoteAllDrafts() {
        translations.forEach(ProgramTranslation::promoteDraftToPublished);
    }

    public void changeApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
    }

    public void changeLocationUrl(String locationUrl) {
        this.locationUrl = locationUrl;
    }
}
