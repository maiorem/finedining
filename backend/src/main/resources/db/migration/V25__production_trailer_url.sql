-- 메인 작품 카드에 예고편 영상(유튜브)으로 바로 가는 버튼을 추가한다(2026-11-01).
-- locationUrl과 같은 취급이다 — 화이트리스트 검증·PIN sudo 모두 없다.
ALTER TABLE production
    ADD COLUMN trailer_url VARCHAR(500) NULL;
