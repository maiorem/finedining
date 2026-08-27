-- 이미지 업로드 파이프라인(CLAUDE.md §7.5). production_id는 의도적으로 FK가 아니라 값이다 —
-- media 패키지를 특정 도메인에 묶지 않고 범용으로 둔다(§6). 리사이즈+JPEG만 만든다 — 진짜
-- 딥줌(DZI)·WebP는 다음 단계로 미뤘다(2026-08-27 결정, §15).

CREATE TABLE media_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    original_key VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500) NULL,
    width INT NULL,
    height INT NULL,
    derivative_640_key VARCHAR(300) NULL,
    derivative_960_key VARCHAR(300) NULL,
    derivative_1600_key VARCHAR(300) NULL,
    lqip_base64 LONGTEXT NULL,
    alt_text VARCHAR(300) NULL,
    published TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_media_asset_production (production_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
