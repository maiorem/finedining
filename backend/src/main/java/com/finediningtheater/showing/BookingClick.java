package com.finediningtheater.showing;

import com.finediningtheater.global.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 예약 링크 클릭 이탈 트래킹 (CLAUDE.md §4). 인증 없는 공개 엔드포인트로 적재되므로
 * 어떤 값도 신뢰하지 않는다 — showingId 존재만 서비스에서 확인하고, FK 연관관계로 만들지 않는다(§7.7).
 */
@Entity
@Getter
@Table(name = "booking_click")
public class BookingClick extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long showingId;

    @Column(length = 40)
    private String channel;

    @Column(length = 10)
    private String locale;

    @Column(length = 100)
    private String utmSource;

    @Column(length = 100)
    private String utmMedium;

    @Column(length = 100)
    private String utmCampaign;

    protected BookingClick() {}

    public BookingClick(
            Long showingId,
            String channel,
            String locale,
            String utmSource,
            String utmMedium,
            String utmCampaign) {
        this.showingId = showingId;
        this.channel = channel;
        this.locale = locale;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
    }
}
