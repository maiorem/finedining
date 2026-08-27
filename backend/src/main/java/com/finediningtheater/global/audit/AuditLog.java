package com.finediningtheater.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * 모든 쓰기의 흔적: 누가·언제·무엇을·이전값→이후값 (CLAUDE.md §7.7). 운영자가 소수라 사고 시
 * 추적이 유일한 복구 수단이다 — 그래서 append-only고 수정 메서드가 없다.
 */
@Entity
@Getter
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @Lob private String beforeJson;

    @Lob private String afterJson;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(
            Long accountId,
            String action,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson,
            String ip) {
        this.accountId = accountId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.ip = ip;
        this.createdAt = Instant.now();
    }
}
