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

    /** 요청 로케일 번역이 없으면 한국어로 폴백한다 (CLAUDE.md §7.6). */
    public ProductionTranslation translationFor(SiteLocale locale) {
        return translations.stream()
                .filter(t -> t.getLocale() == locale)
                .findFirst()
                .or(() -> translations.stream().filter(t -> t.getLocale() == SiteLocale.KO).findFirst())
                .orElse(null);
    }

    public String titleFor(SiteLocale locale) {
        ProductionTranslation translation = translationFor(locale);
        return translation == null ? null : translation.getTitle();
    }

    public ProductionTranslation addTranslation(SiteLocale locale, String title, String subtitle) {
        ProductionTranslation translation = new ProductionTranslation(this, locale, title, subtitle);
        translations.add(translation);
        return translation;
    }
}
