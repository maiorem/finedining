-- 프로그램 부제(2026-09-14 요청서): 목록에는 부제만, 긴 설명은 상세에서만 보인다.
-- 기존 행에는 값이 없으므로 nullable이다(expand 단계, §13.4).
ALTER TABLE program_translation
    ADD COLUMN subtitle VARCHAR(200) NULL,
    ADD COLUMN draft_subtitle VARCHAR(200) NULL;
