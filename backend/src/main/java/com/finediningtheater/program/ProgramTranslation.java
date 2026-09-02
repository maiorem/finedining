package com.finediningtheater.program;

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

/** 프로그램 제목·설명의 로케일별 번역. title/description은 공개본, draft*는 임시저장본이다(§3.9). */
@Entity
@Getter
@Table(
        name = "program_translation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "locale"}))
public class ProgramTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Column(length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 200)
    private String draftTitle;

    @Column(length = 4000)
    private String draftDescription;

    protected ProgramTranslation() {}

    ProgramTranslation(Program program, SiteLocale locale, String title, String description) {
        this.program = program;
        this.locale = locale;
        this.title = title;
        this.description = description;
    }

    public void updateDraft(String title, String description) {
        this.draftTitle = title;
        this.draftDescription = description;
    }

    public void promoteDraftToPublished() {
        if (draftTitle != null) {
            this.title = draftTitle;
            this.description = draftDescription;
        }
    }
}
