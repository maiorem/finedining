package com.finediningtheater.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로컬은 MinIO, dev/prod는 환경변수로 실제 AWS S3를 가리킨다 — 엔드포인트 오버라이드 하나로
 * 같은 코드가 양쪽에 다 붙는다 (CLAUDE.md §7.5·§13.7).
 */
@ConfigurationProperties(prefix = "app.media.s3")
public record MediaProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess,
        String publicBaseUrl,
        boolean autoCreateBucket) {}
