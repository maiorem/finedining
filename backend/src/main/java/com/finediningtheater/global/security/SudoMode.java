package com.finediningtheater.global.security;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * PIN 통과 후 15분간 유지되는 sudo 모드. 만료는 서버가 관리한다 — 클라이언트 타이머를
 * 신뢰하지 않는다 (CLAUDE.md §3.4·§7.4). 발행/삭제/예약 URL 변경/관리자 계정 발급 같은
 * 파괴적·공개적 동작 앞에서 {@link #requireActive(Long)}로 막아 세운다.
 */
@Component
public class SudoMode {

    private static final Duration VALIDITY = Duration.ofMinutes(15);

    private final Cache<Long, Boolean> activeSessions =
            Caffeine.newBuilder().expireAfterWrite(VALIDITY).maximumSize(1_000).build();

    public void activate(Long adminId) {
        activeSessions.put(adminId, Boolean.TRUE);
    }

    public boolean isActive(Long adminId) {
        return activeSessions.getIfPresent(adminId) != null;
    }

    /** sudo가 꺼져 있으면 PIN_REQUIRED를 던진다 — 프론트가 이 코드로 PIN 모달을 띄운다(§7.2). */
    public void requireActive(Long adminId) {
        if (!isActive(adminId)) {
            throw new BusinessException(ErrorCode.PIN_REQUIRED);
        }
    }
}
