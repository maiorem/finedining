package com.finediningtheater.global.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * N회 실패하면 일정 시간 잠그는 범용 카운터. 관리자 로그인(§3.5)과 PIN(§3.4)이 같은 모양의
 * 정책을 쓰므로 공용으로 뺐다. 스프링 빈이 아니다 — 쓰는 쪽(서비스)이 자기 정책값으로 인스턴스를
 * 만들어 필드로 들고 있는다.
 */
public class FailureLockout {

    private final Cache<String, AtomicInteger> failureCounts;
    private final int maxFailures;

    public FailureLockout(int maxFailures, Duration lockoutDuration) {
        this.maxFailures = maxFailures;
        this.failureCounts =
                Caffeine.newBuilder().expireAfterWrite(lockoutDuration).maximumSize(50_000).build();
    }

    public boolean isLocked(String key) {
        AtomicInteger count = failureCounts.getIfPresent(key);
        return count != null && count.get() >= maxFailures;
    }

    public void recordFailure(String key) {
        failureCounts.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public void recordSuccess(String key) {
        failureCounts.invalidate(key);
    }
}
