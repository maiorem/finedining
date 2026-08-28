-- 아티스트 "참여 작품"을 이 사이트에 등록된 Production만 고르는 선택형에서 자유 텍스트로
-- 바꾼다(2026-08-29 결정) — 다른 프로젝트의 참여 이력도 적을 수 있어야 한다. name/role/bio와
-- 같은 공개본+임시저장 패턴을 그대로 따른다(CLAUDE.md §3.9).

ALTER TABLE artist_translation
    ADD COLUMN credits VARCHAR(2000) NULL AFTER bio,
    ADD COLUMN draft_credits VARCHAR(2000) NULL AFTER draft_bio;

-- Artist <-> Production N:M 관계 자체를 제거한다. 아직 오픈 전이라 실 데이터가 없다.
DROP TABLE artist_production;
