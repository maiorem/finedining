-- 2순위(관리자 로그인) 구현: admin_account는 일반 회원(account, 아직 없음 — 카카오는 우선순위
-- 마지막)과 완전히 별개인 테이블이다. 회원가입 절차가 없다 — SUPER_ADMIN이 편집 모드에서 직접
-- 발급한다 (CLAUDE.md §3.1, 2026-08-27 결정).

CREATE TABLE admin_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pin_hash VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 최초 SUPER_ADMIN 시드 (CLAUDE.md §3.1). username=admin / password=ChangeMe!2026 (BCrypt 해시).
-- 개발용 placeholder다 — 반드시 최초 로그인 후 비밀번호를 바꾼다. PIN은 아직 미설정(NULL) —
-- PIN 설정 화면은 다음 하위 단계(PIN sudo 모드)에서 붙인다.
INSERT INTO admin_account (username, password_hash, role, status, pin_hash, created_at, updated_at)
VALUES (
    'admin',
    '$2a$10$/eLBnZdEPAHVSFuebESp5Otc.Mi.JZqGuLFqWvRwQoyPi8uf/NvOK',
    'SUPER_ADMIN',
    'ACTIVE',
    NULL,
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
);
