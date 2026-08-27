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

/**
 * 아티스트 이름·역할·소개의 로케일별 번역. name/role/bio는 공개본, draft*는 임시저장본이다 —
 * 발행해야 draft가 공개본으로 교체된다(CLAUDE.md §3.9, Production과 같은 패턴).
 */
@Entity
@Getter
@Table(
        name = "artist_translation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"artist_id", "locale"}))
public class ArtistTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale locale;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String role;

    @Column(length = 2000)
    private String bio;

    @Column(length = 100)
    private String draftName;

    @Column(length = 100)
    private String draftRole;

    @Column(length = 2000)
    private String draftBio;

    protected ArtistTranslation() {}

    ArtistTranslation(Artist artist, SiteLocale locale, String name, String role, String bio) {
        this.artist = artist;
        this.locale = locale;
        this.name = name;
        this.role = role;
        this.bio = bio;
    }

    public void updateDraft(String name, String role, String bio) {
        this.draftName = name;
        this.draftRole = role;
        this.draftBio = bio;
    }

    public void promoteDraftToPublished() {
        if (draftName != null) {
            this.name = draftName;
            this.role = draftRole;
            this.bio = draftBio;
        }
    }

    public String effectiveName() {
        return draftName != null ? draftName : name;
    }

    public String effectiveRole() {
        return draftName != null ? draftRole : role;
    }

    public String effectiveBio() {
        return draftName != null ? draftBio : bio;
    }
}
