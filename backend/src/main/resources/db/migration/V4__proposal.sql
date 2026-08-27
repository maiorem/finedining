-- 협업제안(기능4). 2026-08-27 결정: 카카오 로그인이 붙기 전까지는 로그인 없이 받는다(§3.7) —
-- account_id는 nullable이고 지금은 항상 NULL이다. 카카오 로그인이 실제로 붙으면 이 컬럼을
-- NOT NULL로 좁히는 후속 마이그레이션이 필요하다(expand-contract, CLAUDE.md §13.4).

CREATE TABLE proposal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    consent_agreed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_proposal_status (status),
    KEY idx_proposal_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
