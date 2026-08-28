package com.finediningtheater.production;

import com.finediningtheater.global.support.BaseTimeEntity;
import com.finediningtheater.global.support.SiteLocale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * 작품 제목·부제·설명의 로케일별 번역. title_ko/title_en을 나란히 두지 않는다 (CLAUDE.md §7.6).
 *
 * <p>title/subtitle/description은 방문자에게 보이는 공개본이고, draft*는 편집 패널의
 * "임시저장" 대상이다. 라이브 사이트에서 편집하므로 이 둘을 분리하지 않으면 작업 중인 문장이
 * 그대로 방문자에게 보인다 — "발행"을 눌러야 draft가 공개본으로 교체된다 (CLAUDE.md §3.9).
 */
@Entity
@Getter
@Table(
        name = "production_translation",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"production_id", "locale"}))
public class ProductionTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Column(length = 200)
    private String title;

    @Column(length = 200)
    private String subtitle;

    @Column(length = 4000)
    private String description;

    @Column(length = 200)
    private String draftTitle;

    @Column(length = 200)
    private String draftSubtitle;

    @Column(length = 4000)
    private String draftDescription;

    protected ProductionTranslation() {}

    // Production.addTranslation()을 통해서만 만든다 — translations 리스트와 어긋나지 않게.
    ProductionTranslation(Production production, SiteLocale locale, String title, String subtitle) {
        this(production, locale, title, subtitle, null);
    }

    ProductionTranslation(
            Production production, SiteLocale locale, String title, String subtitle, String description) {
        this.production = production;
        this.locale = locale;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    public void updateDraft(String title, String subtitle) {
        updateDraft(title, subtitle, this.draftDescription);
    }

    public void updateDraft(String title, String subtitle, String description) {
        this.draftTitle = title;
        this.draftSubtitle = subtitle;
        this.draftDescription = description;
    }

    /** 발행 시 draft가 있으면 공개본으로 교체한다. draft가 없으면(임시저장 없이 발행) 기존 공개본을 그대로 둔다. */
    public void promoteDraftToPublished() {
        if (draftTitle != null) {
            this.title = draftTitle;
            this.subtitle = draftSubtitle;
            this.description = draftDescription;
        }
    }

    /** 편집 패널에 채워 넣을 값 — draft가 있으면 draft, 없으면 지금 공개본. */
    public String effectiveTitle() {
        return draftTitle != null ? draftTitle : title;
    }

    public String effectiveSubtitle() {
        return draftTitle != null ? draftSubtitle : subtitle;
    }

    public String effectiveDescription() {
        return draftTitle != null ? draftDescription : description;
    }
}
