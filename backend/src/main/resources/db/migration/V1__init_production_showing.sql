-- 1순위(예약) 구현: Production/Showing/BookingClick 스키마 + 플레이스홀더 시드 데이터.
-- 관리자 편집 API(2순위: 관리자 로그인 + 작품 아카이빙)가 붙기 전까지는 회차 데이터를
-- 이 마이그레이션으로만 채운다 (프로젝트 메모리 booking-step1-seed-approach 참고).
-- 아래 작품명·회차 일정은 실제 콘텐츠가 아니라 캘린더 UI 검증용 placeholder다.

CREATE TABLE production (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(191) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_production_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE production_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(200) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_production_translation_production_locale (production_id, locale),
    CONSTRAINT fk_production_translation_production
        FOREIGN KEY (production_id) REFERENCES production (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE showing (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    duration_minutes INT NOT NULL,
    venue_name VARCHAR(200) NOT NULL,
    venue_address VARCHAR(300) NULL,
    spoken_language VARCHAR(10) NOT NULL,
    interpretation_available TINYINT(1) NOT NULL DEFAULT 0,
    sales_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    booking_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_showing_status_starts_at (status, starts_at),
    CONSTRAINT fk_showing_production
        FOREIGN KEY (production_id) REFERENCES production (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- booking_click은 인증 없는 공개 엔드포인트가 적재하므로 FK를 걸지 않는다 (CLAUDE.md §7.7).
CREATE TABLE booking_click (
    id BIGINT NOT NULL AUTO_INCREMENT,
    showing_id BIGINT NOT NULL,
    channel VARCHAR(40) NULL,
    locale VARCHAR(10) NULL,
    utm_source VARCHAR(100) NULL,
    utm_medium VARCHAR(100) NULL,
    utm_campaign VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_booking_click_showing (showing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ── 플레이스홀더 시드 데이터 ──────────────────────────────────────────
-- "showcase" 작품 1건 + 판매 상태·언어 조합이 다른 회차 5건 (캘린더 배지 검증용).
-- 시각은 UTC로 저장한다. 10:00 UTC = 19:00 KST (CLAUDE.md §13.4).

INSERT INTO production (slug, status, published_at, published_by, created_at, updated_at)
VALUES ('showcase', 'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO production_translation (production_id, locale, title, subtitle, created_at, updated_at)
SELECT id, 'KO', '시어터 디너 쇼케이스', '공연과 미식이 만나는 무대', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

INSERT INTO production_translation (production_id, locale, title, subtitle, created_at, updated_at)
SELECT id, 'EN', 'Theater Dinner Showcase', 'Where performance meets fine dining', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

-- 1) OPEN, 국문 진행
INSERT INTO showing (
    production_id, starts_at, duration_minutes, venue_name, venue_address,
    spoken_language, interpretation_available, sales_status, booking_url,
    status, published_at, published_by, created_at, updated_at
)
SELECT id, '2026-09-25 10:00:00.000000', 120, '공연장 A (placeholder)', '서울특별시 종로구 예시로 10',
       'KO', 0, 'OPEN', 'https://booking.naver.com/booking/13/bizes/000000/items/0000001',
       'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

-- 2) CLOSING_SOON
INSERT INTO showing (
    production_id, starts_at, duration_minutes, venue_name, venue_address,
    spoken_language, interpretation_available, sales_status, booking_url,
    status, published_at, published_by, created_at, updated_at
)
SELECT id, '2026-09-26 10:00:00.000000', 120, '공연장 A (placeholder)', '서울특별시 종로구 예시로 10',
       'KO', 0, 'CLOSING_SOON', 'https://booking.naver.com/booking/13/bizes/000000/items/0000002',
       'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

-- 3) SOLD_OUT (예약 URL은 있어도 버튼은 비활성 처리됨 — Showing.isBookingAvailable() 참고)
INSERT INTO showing (
    production_id, starts_at, duration_minutes, venue_name, venue_address,
    spoken_language, interpretation_available, sales_status, booking_url,
    status, published_at, published_by, created_at, updated_at
)
SELECT id, '2026-09-27 10:00:00.000000', 120, '공연장 A (placeholder)', '서울특별시 종로구 예시로 10',
       'KO', 0, 'SOLD_OUT', 'https://booking.naver.com/booking/13/bizes/000000/items/0000003',
       'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

-- 4) OPEN, 영문 진행 + 통역 제공
INSERT INTO showing (
    production_id, starts_at, duration_minutes, venue_name, venue_address,
    spoken_language, interpretation_available, sales_status, booking_url,
    status, published_at, published_by, created_at, updated_at
)
SELECT id, '2026-10-02 10:00:00.000000', 120, '공연장 B (placeholder)', '서울특별시 중구 예시로 20',
       'EN', 1, 'OPEN', 'https://booking.naver.com/booking/13/bizes/000000/items/0000004',
       'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';

-- 5) OPEN이지만 예약 URL 미배정 (버튼 비활성 상태 검증용)
INSERT INTO showing (
    production_id, starts_at, duration_minutes, venue_name, venue_address,
    spoken_language, interpretation_available, sales_status, booking_url,
    status, published_at, published_by, created_at, updated_at
)
SELECT id, '2026-10-03 10:00:00.000000', 120, '공연장 A (placeholder)', '서울특별시 종로구 예시로 10',
       'KO', 0, 'OPEN', NULL,
       'PUBLISHED', UTC_TIMESTAMP(6), NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM production WHERE slug = 'showcase';
