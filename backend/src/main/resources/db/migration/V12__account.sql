-- 카카오로 로그인하는 일반 회원(CLAUDE.md §3.1). admin_account와 완전히 별개다 — 가입
-- 절차·인증 수단·테이블이 전부 다르다. role 컬럼이 없다 — 로그인 자체가 곧 리뷰 작성 자격이다.

CREATE TABLE account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    -- 탈퇴 시 null로 익명화한다(§3.2) — MySQL은 NULL을 유니크 제약에서 서로 다른 값으로
    -- 취급하므로 탈퇴한 계정이 여러 개 쌓여도 재로그인(새 계정 생성)을 막지 않는다.
    provider_user_id VARCHAR(191) NULL,
    email VARCHAR(255) NULL,
    nickname VARCHAR(100) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'KO',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_provider_user (provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- INVITE_ONLY 정책에서만 참조된다(§3.2). 관리자와는 무관한 별도 테이블이다 — 예전
-- account_invite와 헷갈리지 않게 이름을 다시 쓰지 않는다.
CREATE TABLE signup_allowlist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NULL,
    provider_user_id VARCHAR(191) NULL,
    email VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
