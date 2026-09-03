-- 협업 제안 폼에 비즈니스 카테고리를 추가한다(2026-09-04, 홈페이지 구성 요청사항 §6).
-- 과거 제안에는 카테고리가 없었으므로 NULL을 허용한다 — 새 제출은 요청 DTO 검증(@NotNull)이
-- 항상 값을 채우도록 강제하지만, 기존 행까지 억지로 채울 근거값이 없다.
ALTER TABLE proposal
    ADD COLUMN category VARCHAR(30) NULL AFTER contact_email;
