-- 프로그램에 이미지 갤러리(대표 이미지 + 캡션이 있는 본문 이미지)를 지원하려면 Production처럼
-- 슬러그 기반 상세 페이지가 필요하다. 기존 행은 id로 슬러그를 채운 뒤 NOT NULL로 잠근다
-- (CLAUDE.md §13.4 expand-contract — 아직 실 데이터가 없어 한 마이그레이션 안에서 처리해도 안전하다).
ALTER TABLE program ADD COLUMN slug VARCHAR(191) NULL AFTER id;

UPDATE program SET slug = CONCAT('program-', id) WHERE slug IS NULL;

ALTER TABLE program
    MODIFY COLUMN slug VARCHAR(191) NOT NULL,
    ADD UNIQUE KEY uk_program_slug (slug);
