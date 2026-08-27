package com.finediningtheater.global.security;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.FailureLockout;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 6자리 PIN의 해시·검증·강도 정책·잠금을 다룬다 (CLAUDE.md §3.4). 생년월일 거부는 관리자 계정에
 * 생년월일을 아예 수집하지 않아 적용 대상이 없다 — 나머지 규칙(단순 패턴)만 검사한다.
 */
@Component
public class PinAuth {

    private static final int MAX_FAILURES = 5;
    // "재로그인으로만 해제"이지 시간이 지나면 풀리는 게 아니다 — AdminAuthService.login()
    // 성공 시에만 clearLockout()으로 초기화한다. 여기 만료 시간은 메모리 누수 방지용 상한일 뿐이다.
    private static final Duration FAILURE_RETENTION = Duration.ofDays(365);

    private final PasswordEncoder passwordEncoder;
    private final FailureLockout lockout = new FailureLockout(MAX_FAILURES, FAILURE_RETENTION);

    public PinAuth(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPin) {
        validateStrength(rawPin);
        return passwordEncoder.encode(rawPin);
    }

    /** 잠겨 있으면 PIN이 맞아도 거부한다 — 재로그인 전에는 절대 안 풀린다. */
    public void verify(Long adminId, String storedHash, String rawPin) {
        String key = String.valueOf(adminId);
        if (lockout.isLocked(key)) {
            throw new BusinessException(ErrorCode.PIN_LOCKED);
        }
        if (storedHash == null || !passwordEncoder.matches(rawPin, storedHash)) {
            lockout.recordFailure(key);
            throw new BusinessException(ErrorCode.PIN_INVALID);
        }
        lockout.recordSuccess(key);
    }

    public void clearLockout(Long adminId) {
        lockout.recordSuccess(String.valueOf(adminId));
    }

    private void validateStrength(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.WEAK_PIN, "6자리 숫자로 입력해 주세요.");
        }
        if (isAllSameDigit(pin) || isSequential(pin)) {
            throw new BusinessException(ErrorCode.WEAK_PIN);
        }
    }

    private boolean isAllSameDigit(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private boolean isSequential(String pin) {
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < pin.length(); i++) {
            int prev = pin.charAt(i - 1) - '0';
            int curr = pin.charAt(i) - '0';
            ascending = ascending && curr == prev + 1;
            descending = descending && curr == prev - 1;
        }
        return ascending || descending;
    }
}
