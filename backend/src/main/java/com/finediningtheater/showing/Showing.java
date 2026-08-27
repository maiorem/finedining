package com.finediningtheater.showing;

import com.finediningtheater.global.support.Publishable;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
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
import java.time.Instant;
import lombok.Getter;

/** 특정 일시·장소의 실제 공연 1회 (CLAUDE.md §1·§4). 좌석·정원은 다루지 않는다 — 재고의 진실은 네이버 예약에 있다. */
@Entity
@Getter
@Table(name = "showing")
public class Showing extends Publishable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false, length = 200)
    private String venueName;

    @Column(length = 300)
    private String venueAddress;

    /** 회차 진행 언어. 번역(SiteLocale)과는 다른 개념 — 영문 관광객에게 한국어 전용 회차를 예약 가능한 것처럼 보이면 사고가 난다 (CLAUDE.md §7.6). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteLocale spokenLanguage;

    @Column(nullable = false)
    private boolean interpretationAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesStatus salesStatus = SalesStatus.OPEN;

    @Column(length = 500)
    private String bookingUrl;

    protected Showing() {}

    public Showing(
            Production production,
            Instant startsAt,
            int durationMinutes,
            String venueName,
            String venueAddress,
            SiteLocale spokenLanguage,
            boolean interpretationAvailable) {
        this.production = production;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.spokenLanguage = spokenLanguage;
        this.interpretationAvailable = interpretationAvailable;
    }

    public void changeSalesStatus(SalesStatus salesStatus) {
        this.salesStatus = salesStatus;
    }

    public void changeBookingUrl(String bookingUrl) {
        this.bookingUrl = bookingUrl;
    }

    /** 예약 URL이 없거나 판매가 끝났으면 버튼을 비활성화한다 (CLAUDE.md §4). */
    public boolean isBookingAvailable() {
        return bookingUrl != null
                && !bookingUrl.isBlank()
                && (salesStatus == SalesStatus.OPEN || salesStatus == SalesStatus.CLOSING_SOON);
    }
}
