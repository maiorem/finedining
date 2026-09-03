-- 보도자료(소개 페이지 탭, 2026-09-04 결정 — CLAUDE.md §17에서 "새 기능으로 보류"였다가
-- 확정됨). Casting과 같은 열람전용·슬러그없음 패턴이지만 번역 테이블은 없다 — 실제 언론사
-- 기사 제목은 원문 그대로 노출하고 로케일별로 번역하지 않는다.

CREATE TABLE press_clipping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    external_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    published_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_press_clipping_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
