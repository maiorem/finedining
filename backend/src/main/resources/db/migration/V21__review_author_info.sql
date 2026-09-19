-- 이야기(리뷰) 작성란 확장(2026-09-14 요청서): 감사 리워드 연락용 이름·연락처와 개인정보 수집 동의 시각.
-- 기존 글에는 값이 없으므로 전부 nullable이다(expand 단계, §13.4). 관리자에게만 보이는 값이다.
ALTER TABLE review
    ADD COLUMN author_name VARCHAR(50) NULL,
    ADD COLUMN contact VARCHAR(100) NULL,
    ADD COLUMN privacy_consent_at DATETIME(6) NULL;
