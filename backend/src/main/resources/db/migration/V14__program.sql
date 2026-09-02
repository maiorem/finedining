-- 프로그램(이벤트 공지). Casting과 같은 패턴(슬러그 없음, 목록 전용) + Artist.link_url 패턴의
-- 링크 필드 2개(참가하기/위치보기).

CREATE TABLE program (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apply_url VARCHAR(500) NULL,
    location_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE program_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    program_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(200) NULL,
    description VARCHAR(4000) NULL,
    draft_title VARCHAR(200) NULL,
    draft_description VARCHAR(4000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_program_translation_program_locale (program_id, locale),
    CONSTRAINT fk_program_translation_program FOREIGN KEY (program_id) REFERENCES program (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
