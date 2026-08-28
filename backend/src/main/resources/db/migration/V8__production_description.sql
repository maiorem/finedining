-- 작품 설명(긴 텍스트). CLAUDE.md §2가 요구하는 "이미지+설명(텍스트)+캡션" 중
-- 빠져 있던 설명 필드를 채운다. draft/publish 패턴을 title/subtitle과 동일하게 따른다(§3.9).

ALTER TABLE production_translation
    ADD COLUMN description VARCHAR(4000) NULL AFTER subtitle,
    ADD COLUMN draft_description VARCHAR(4000) NULL AFTER draft_subtitle;
