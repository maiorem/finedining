package com.finediningtheater.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis를 두지 않는다 — 이 규모(작품 수십, 회차 수백, 운영자 5명)에서는 순수 부채다
 * (CLAUDE.md §5). 공개 조회 응답을 이 캐시로 감싼다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
        return cacheManager ->
                cacheManager.setCaffeine(
                        Caffeine.newBuilder().maximumSize(1_000).expireAfterWrite(1, TimeUnit.HOURS));
    }
}
