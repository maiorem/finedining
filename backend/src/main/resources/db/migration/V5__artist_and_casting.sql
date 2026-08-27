-- 아티스트(기능6): 창작자 프로필 + 모집 공고. Production과 같은 패턴 —
-- title(공개본)/draft*(임시저장) 분리로 라이브 편집 중 초안이 새지 않는다(CLAUDE.md §3.9).
-- 사진(이미지 파이프라인)은 이번에도 뺀다 — S3가 아직 없다.

CREATE TABLE artist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(191) NOT NULL,
    link_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_artist_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE artist_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    artist_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(100) NULL,
    role VARCHAR(100) NULL,
    bio VARCHAR(2000) NULL,
    draft_name VARCHAR(100) NULL,
    draft_role VARCHAR(100) NULL,
    draft_bio VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_artist_translation_artist_locale (artist_id, locale),
    CONSTRAINT fk_artist_translation_artist FOREIGN KEY (artist_id) REFERENCES artist (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Artist <-> Production N:M (CLAUDE.md §3.8).
CREATE TABLE artist_production (
    artist_id BIGINT NOT NULL,
    production_id BIGINT NOT NULL,
    PRIMARY KEY (artist_id, production_id),
    CONSTRAINT fk_artist_production_artist FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_artist_production_production FOREIGN KEY (production_id) REFERENCES production (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 모집 공고. 슬러그 없음 — 개별 상세 페이지 없이 목록으로만 노출된다.
CREATE TABLE casting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE casting_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    casting_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(200) NULL,
    body VARCHAR(4000) NULL,
    draft_title VARCHAR(200) NULL,
    draft_body VARCHAR(4000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_casting_translation_casting_locale (casting_id, locale),
    CONSTRAINT fk_casting_translation_casting FOREIGN KEY (casting_id) REFERENCES casting (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
