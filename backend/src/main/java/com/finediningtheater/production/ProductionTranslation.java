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

/** 작품 제목·부제의 로케일별 번역. title_ko/title_en을 나란히 두지 않는다 (CLAUDE.md §7.6). */
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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String subtitle;

    protected ProductionTranslation() {}

    // Production.addTranslation()을 통해서만 만든다 — translations 리스트와 어긋나지 않게.
    ProductionTranslation(Production production, SiteLocale locale, String title, String subtitle) {
        this.production = production;
        this.locale = locale;
        this.title = title;
        this.subtitle = subtitle;
    }
}
