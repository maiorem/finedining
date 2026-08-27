package com.finediningtheater.global.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * 공개 API 중 GET이 아닌 예외 엔드포인트(예약 클릭 트래킹, 문의 등록)에 거는 IP 단위 레이트리밋
 * (CLAUDE.md §7.7). Redis 없이 Caffeine 하나로 충분한 규모다(§5).
 */
@Component
public class RateLimiter {

    private final Cache<String, AtomicInteger> counters =
            Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(50_000).build();

    public boolean tryAcquire(String key, int maxPerMinute) {
        AtomicInteger counter = counters.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= maxPerMinute;
    }
}
