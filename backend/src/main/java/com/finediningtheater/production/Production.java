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

    @OneToMany(mappedBy = "production", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductionTranslation> translations = new ArrayList<>();

    protected Production() {}

    public Production(String slug) {
        this.slug = slug;
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
        ProductionTranslation translation = new ProductionTranslation(this, locale, title, subtitle);
        translations.add(translation);
        return translation;
    }

    /** 발행 시 모든 로케일의 draft를 공개본으로 승격한다 (CLAUDE.md §3.9). */
    public void promoteAllDrafts() {
        translations.forEach(ProductionTranslation::promoteDraftToPublished);
    }
}
