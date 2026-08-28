package com.finediningtheater.about;

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
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * 기업·창작집단 소개(CLAUDE.md §1 핵심 목적 ①, §6 "집단 소개 콘텐츠"). 회사가 하나뿐이라
 * 이 엔티티는 항상 정확히 한 행만 존재하는 싱글턴이다 — Flyway가 최초 행을 심고(V11),
 * 관리자는 새로 만들지 않고 그 행만 편집한다. Casting과 같은 draft/publish 패턴을 쓴다(§3.9).
 */
@Entity
@Getter
@Table(name = "about_content")
public class AboutContent extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "aboutContent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<AboutTranslation> translations = new HashSet<>();

    public AboutTranslation translationRowFor(SiteLocale locale) {
        return translations.stream().filter(t -> t.getLocale() == locale).findFirst().orElse(null);
    }

    /** 공개 조회 전용 — 공개본 소개문이 없는 로케일은 한국어로 폴백한다(§7.6). */
    public AboutTranslation translationFor(SiteLocale locale) {
        return translations.stream()
                .filter(t -> t.getLocale() == locale && t.getIntro() != null)
                .findFirst()
                .or(
                        () ->
                                translations.stream()
                                        .filter(t -> t.getLocale() == SiteLocale.KO && t.getIntro() != null)
                                        .findFirst())
                .orElse(null);
    }

    public AboutTranslation addTranslation(SiteLocale locale, String intro) {
        AboutTranslation translation = new AboutTranslation(this, locale, intro);
        translations.add(translation);
        return translation;
    }

    public void promoteAllDrafts() {
        translations.forEach(AboutTranslation::promoteDraftToPublished);
    }
}
