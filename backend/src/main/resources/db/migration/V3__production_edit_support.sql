-- 2순위(작품 아카이빙, 텍스트만): 작품 편집 모드 + 감사 로그.
--
-- production_translation.title/subtitle은 지금까지 "공개본"으로만 썼는데, 이제부터는 초안
-- 상태에서 아직 한 번도 발행되지 않은 로케일도 행으로 존재할 수 있어 NOT NULL을 뗀다.
-- draft_title/draft_subtitle은 "임시저장" 전용 — 발행(publish) 시에만 공개본으로 복사된다
-- (CLAUDE.md §3.9, 라이브 사이트에서 편집하므로 작업 중인 문장이 그대로 방문자에게 보이면 안 된다).

ALTER TABLE production_translation
    MODIFY title VARCHAR(200) NULL,
    ADD COLUMN draft_title VARCHAR(200) NULL,
    ADD COLUMN draft_subtitle VARCHAR(200) NULL;

-- 모든 쓰기의 흔적 (CLAUDE.md §7.7). 운영자가 소수라 사고 시 추적이 유일한 복구 수단이다.
CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    before_json LONGTEXT NULL,
    after_json LONGTEXT NULL,
    ip VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_log_target (target_type, target_id),
    KEY idx_audit_log_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
