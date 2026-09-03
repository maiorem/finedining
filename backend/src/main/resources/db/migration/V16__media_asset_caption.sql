-- 작품·프로그램 상세를 블로그처럼 이미지+설명 문단으로 보여주기 위한 필드(2026-09-04 결정).
-- alt_text는 접근성용 짧은 대체 텍스트로 남기고, 방문자에게 실제로 보이는 설명 문단은 이
-- 별도 컬럼에 둔다 — 지금까지 alt_text를 화면에 캡션으로 노출하던 걸 완전히 분리한다.
ALTER TABLE media_asset
    ADD COLUMN caption VARCHAR(2000) NULL AFTER alt_text;
