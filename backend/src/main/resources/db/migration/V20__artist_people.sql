-- 사람들(PEOPLE) 개편(2026-09-14 요청서): 카드에 한마디·이메일, 인터뷰 영상 링크, 노출 순서를 얻는다.
-- 인터뷰 글이 길어질 수 있어 bio를 넓힌다(2000→4000, 행 크기 한도 때문에 이 이상은 TEXT가 필요하다. 넓히기만 하므로 이전 이미지와 호환된다, §13.4).
ALTER TABLE artist
    ADD COLUMN email VARCHAR(200) NULL,
    ADD COLUMN display_order INT NOT NULL DEFAULT 0,
    ADD COLUMN interview_url VARCHAR(500) NULL;

ALTER TABLE artist_translation
    ADD COLUMN quote VARCHAR(500) NULL,
    ADD COLUMN draft_quote VARCHAR(500) NULL,
    MODIFY COLUMN bio VARCHAR(4000) NULL,
    MODIFY COLUMN draft_bio VARCHAR(4000) NULL;
