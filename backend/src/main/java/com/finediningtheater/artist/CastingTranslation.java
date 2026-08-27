package com.finediningtheater.artist;

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

/** 모집 공고 제목·본문의 로케일별 번역. title/body는 공개본, draft*는 임시저장본이다(§3.9). */
@Entity
@Getter
@Table(
        name = "casting_translation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"casting_id", "locale"}))
public class CastingTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "casting_id", nullable = false)
    private Casting casting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Column(length = 200)
    private String title;

    @Column(length = 4000)
    private String body;

    @Column(length = 200)
    private String draftTitle;

    @Column(length = 4000)
    private String draftBody;

    protected CastingTranslation() {}

    CastingTranslation(Casting casting, SiteLocale locale, String title, String body) {
        this.casting = casting;
        this.locale = locale;
        this.title = title;
        this.body = body;
    }

    public void updateDraft(String title, String body) {
        this.draftTitle = title;
        this.draftBody = body;
    }

    public void promoteDraftToPublished() {
        if (draftTitle != null) {
            this.title = draftTitle;
            this.body = draftBody;
        }
    }
}
