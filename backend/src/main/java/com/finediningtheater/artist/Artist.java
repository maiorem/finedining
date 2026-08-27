package com.finediningtheater.artist;

import com.finediningtheater.global.support.Publishable;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * 창작자 프로필(기능6). 계정이 아니다 — 로그인하는 주체가 아니라 소개되는 대상이다(CLAUDE.md §1·§3.8).
 * 하트(좋아요)는 범위에서 뺐다(2026-08-26) — 로그인 종속 기능이 없어 완전히 공개다.
 */
@Entity
@Getter
@Table(name = "artist")
public class Artist extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String slug;

    /** 웹사이트·SNS 등 외부 링크 하나. 로케일과 무관해 번역 테이블에 두지 않는다. */
    @Column(length = 500)
    private String linkUrl;

    // Set이다 — List(bag)로 두면 productions.translations(다른 bag)와 한 쿼리에서 fetch join할 때
    // Hibernate가 MultipleBagFetchException을 던진다(2026-08-27 발견). 번역은 순서가 의미 없으니
    // Set이 자연스럽기도 하다.
    @OneToMany(mappedBy = "artist", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ArtistTranslation> translations = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "artist_production",
            joinColumns = @JoinColumn(name = "artist_id"),
            inverseJoinColumns = @JoinColumn(name = "production_id"))
    private Set<Production> productions = new HashSet<>();

    protected Artist() {}

    public Artist(String slug) {
        this.slug = slug;
    }

    /** 공개 조회 전용 — 공개본 이름이 없는(초안뿐인) 로케일은 한국어로 폴백한다(§7.6). */
    public ArtistTranslation translationFor(SiteLocale locale) {
        return translations.stream()
                .filter(t -> t.getLocale() == locale && t.getName() != null)
                .findFirst()
                .or(
                        () ->
                                translations.stream()
                                        .filter(t -> t.getLocale() == SiteLocale.KO && t.getName() != null)
                                        .findFirst())
                .orElse(null);
    }

    public String nameFor(SiteLocale locale) {
        ArtistTranslation translation = translationFor(locale);
        return translation == null ? null : translation.getName();
    }

    /** 편집용. 공개 여부와 무관하게 정확히 그 로케일 행을 찾는다. */
    public ArtistTranslation translationRowFor(SiteLocale locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst().orElse(null);
    }

    public ArtistTranslation addTranslation(SiteLocale locale, String name, String role, String bio) {
        ArtistTranslation translation = new ArtistTranslation(this, locale, name, role, bio);
        translations.add(translation);
        return translation;
    }

    public void promoteAllDrafts() {
        translations.forEach(ArtistTranslation::promoteDraftToPublished);
    }

    public void changeLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void replaceProductions(Set<Production> newProductions) {
        this.productions.clear();
        this.productions.addAll(newProductions);
    }
}
