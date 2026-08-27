package com.finediningtheater.media;

import com.finediningtheater.global.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * 작품에 붙는 이미지 한 장. Production과의 관계는 의도적으로 JPA 연관관계가 아니라 순수
 * {@code productionId} 값이다 — media 패키지를 도메인에 묶지 않고 범용으로 둔다(CLAUDE.md §6).
 * published는 Production의 draft/publish 패턴을 그대로 따른다: 업로드 직후에는 false이고,
 * 작품이 (재)발행될 때 함께 true로 승격된다 — 그래야 편집 중인 이미지가 방문자에게 새지 않는다.
 */
@Entity
@Getter
@Table(name = "media_asset")
public class MediaAsset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productionId;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 300)
    private String originalKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaAssetStatus status = MediaAssetStatus.PENDING;

    @Column(length = 500)
    private String failureReason;

    private Integer width;

    private Integer height;

    // 숫자 뒤 대문자 앞에는 Hibernate 기본 네이밍 전략이 밑줄을 넣지 않는다
    // (derivative1600Key → derivative1600key) — 마이그레이션과 어긋나므로 명시한다.
    @Column(name = "derivative_640_key", length = 300)
    private String derivative640Key;

    @Column(name = "derivative_960_key", length = 300)
    private String derivative960Key;

    @Column(name = "derivative_1600_key", length = 300)
    private String derivative1600Key;

    @Lob
    private String lqipBase64;

    @Column(length = 300)
    private String altText;

    @Column(nullable = false)
    private boolean published;

    protected MediaAsset() {}

    public MediaAsset(Long productionId, int sortOrder, String originalKey) {
        this.productionId = productionId;
        this.sortOrder = sortOrder;
        this.originalKey = originalKey;
    }

    public void markReady(
            int width,
            int height,
            String derivative640Key,
            String derivative960Key,
            String derivative1600Key,
            String lqipBase64,
            String altText) {
        this.status = MediaAssetStatus.READY;
        this.width = width;
        this.height = height;
        this.derivative640Key = derivative640Key;
        this.derivative960Key = derivative960Key;
        this.derivative1600Key = derivative1600Key;
        this.lqipBase64 = lqipBase64;
        this.altText = altText;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = MediaAssetStatus.FAILED;
        this.failureReason = reason;
    }

    /** 작품 발행 시 호출된다 — READY 상태의 이미지만 공개된다(§3.9의 draft/publish 패턴). */
    public void publish() {
        this.published = true;
    }

    public List<String> allObjectKeys() {
        return Stream.of(originalKey, derivative640Key, derivative960Key, derivative1600Key)
                .filter(Objects::nonNull)
                .toList();
    }
}
