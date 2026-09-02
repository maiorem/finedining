package com.finediningtheater.production;

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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** 작품(IP). 공연+식사가 결합된 최상위 콘텐츠 단위 (CLAUDE.md §1). */
@Entity
@Getter
@Table(name = "production")
public class Production extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String slug;

    /** 네이버 예약 URL. 캘린더는 만들지 않는다 — 네이버 예약이 이미 제공한다. 저장 시 호스트 화이트리스트로 검증한다(§4). */
    @Column(length = 500)
    private String bookingUrl;

    /** 공연장 위치 링크(지도 등). 화이트리스트 검증 없음 — Artist.linkUrl과 같은 취급이다. */
    @Column(length = 500)
    private String locationUrl;

    @OneToMany(mappedBy = "production", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductionTranslation> translations = new ArrayList<>();

    protected Production() {}

    public Production(String slug) {
        this.slug = slug;
    }

    /** 예약 URL 변경은 §3.4의 PIN 필수 목록에 있다 — 호출부(EditController)가 sudo 모드를 강제한다. */
    public void changeBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }

    public void changeLocationUrl(String locationUrl) {
        this.locationUrl = locationUrl;
    }

    /**
     * 공개 조회 전용. 요청 로케일에 공개본 제목이 없으면(아직 미발행) 한국어로 폴백한다
     * (CLAUDE.md §7.6). draft만 있고 title이 null인 행은 여기서 걸러진다 — 초안이 방문자에게
     * 보이면 안 되기 때문이다(§3.9).
     */
    public ProductionTranslation translationFor(SiteLocale locale) {
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
        ProductionTranslation translation = translationFor(locale);
        return translation == null ? null : translation.getTitle();
    }

    /** 편집용. 공개 여부와 무관하게 정확히 그 로케일 행을 찾는다 — 없으면 null. */
    public ProductionTranslation translationRowFor(SiteLocale locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst().orElse(null);
    }

    public ProductionTranslation addTranslation(SiteLocale locale, String title, String subtitle) {
        return addTranslation(locale, title, subtitle, null);
    }

    public ProductionTranslation addTranslation(SiteLocale locale, String title, String subtitle, String description) {
        ProductionTranslation translation = new ProductionTranslation(this, locale, title, subtitle, description);
        translations.add(translation);
        return translation;
    }

    /** 발행 시 모든 로케일의 draft를 공개본으로 승격한다 (CLAUDE.md §3.9). */
    public void promoteAllDrafts() {
        translations.forEach(ProductionTranslation::promoteDraftToPublished);
    }
}
