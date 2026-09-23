-- 예약하기 상세에서 제목과 함께 보이는 큰 이미지를 목록 대표사진과 따로 고를 수 있게 한다
-- (2026-09-23). 지정하지 않으면(NULL) 지금처럼 목록 대표사진(첫 번째 이미지)을 그대로 쓴다.
-- media_asset은 owner_type+owner_id로만 연결되는 범용 테이블이라(CLAUDE.md §6) FK를 걸지 않는다 —
-- 지정한 사진이 나중에 삭제돼도 조회 시점에 조용히 대표사진으로 되돌아간다(ProductionController).
ALTER TABLE production
    ADD COLUMN hero_image_id BIGINT NULL;
