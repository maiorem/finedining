-- 소개(About, CLAUDE.md §6 "집단 소개 콘텐츠"). 회사가 하나뿐이니 about_content는 항상
-- 정확히 한 행만 갖는 싱글턴이다. Casting과 같은 draft/publish 패턴을 쓴다(§3.9).
-- 연혁(History)은 이번 범위에서 뺐다(2026-08-29 결정) — 소개문만 먼저 채운다.

CREATE TABLE about_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE about_translation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    about_content_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    intro VARCHAR(4000) NULL,
    draft_intro VARCHAR(4000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_about_translation_content_locale (about_content_id, locale),
    CONSTRAINT fk_about_translation_content FOREIGN KEY (about_content_id) REFERENCES about_content (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 싱글턴 행을 심는다 — 관리자는 이 행을 만드는 게 아니라 항상 이 하나만 편집한다.
INSERT INTO about_content (status, created_at, updated_at)
VALUES ('DRAFT', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
