-- 사이트 전역 설정(2026-09-19). 지금은 공개 여부 하나뿐이다 — 오픈 전에는 관리자만 볼 수 있게
-- 비공개로 두고, 오픈 때 관리자 화면 버튼으로 공개로 바꾼다. 기본값을 비공개로 심는 이유는
-- 이 마이그레이션이 적용되는 순간 사이트가 외부에 노출되는 사고를 막기 위해서다(안전한 쪽 기본값).

CREATE TABLE site_setting (
    setting_key VARCHAR(50) NOT NULL,
    setting_value VARCHAR(200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO site_setting (setting_key, setting_value, created_at, updated_at)
VALUES ('SITE_PUBLIC', 'false', NOW(6), NOW(6));
