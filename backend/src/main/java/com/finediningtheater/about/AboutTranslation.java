package com.finediningtheater.about;

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

/** 소개문의 로케일별 번역. intro는 공개본, draftIntro는 임시저장본이다(CLAUDE.md §3.9). */
@Entity
@Getter
@Table(
        name = "about_translation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"about_content_id", "locale"}))
public class AboutTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "about_content_id", nullable = false)
    private AboutContent aboutContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Column(length = 4000)
    private String intro;

    @Column(length = 4000)
    private String draftIntro;

    protected AboutTranslation() {}

    AboutTranslation(AboutContent aboutContent, SiteLocale locale, String intro) {
        this.aboutContent = aboutContent;
        this.locale = locale;
        this.intro = intro;
    }

    public void updateDraft(String intro) {
        this.draftIntro = intro;
    }

    public void promoteDraftToPublished() {
        if (draftIntro != null) {
            this.intro = draftIntro;
        }
    }
}
