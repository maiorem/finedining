package com.finediningtheater.global.support;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

/**
 * 발행 대상 엔티티의 공통 상태. publishedBy는 계정 엔티티가 아직 없는 단계에서도 독립적으로
 * 동작해야 하므로 연관관계가 아니라 account id 값으로만 보관한다 (CLAUDE.md §7.3).
 */
@Getter
@MappedSuperclass
public abstract class Publishable extends BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status = ContentStatus.DRAFT;

    private Instant publishedAt;

    private Long publishedBy;

    public void publish(Long accountId) {
        this.status = ContentStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.publishedBy = accountId;
    }

    public void unpublish() {
        this.status = ContentStatus.DRAFT;
        this.publishedAt = null;
        this.publishedBy = null;
    }

    public boolean isPublished() {
        return status == ContentStatus.PUBLISHED;
    }
}
