-- 리뷰(요구서상 커뮤니티, 기능7). account_id는 FK가 아니라 값이다 — 카카오 로그인(계정 테이블
-- 자체)이 아직 없다. 로그인이 붙으면 실제 FK로 승격한다(CLAUDE.md §13.4 expand-contract).
--
-- 작성 경로가 아직 없어(카카오 로그인이 우선순위 맨 마지막) 관리자 모더레이션 화면을 눈으로
-- 확인할 placeholder 리뷰 몇 건을 심는다 — booking 1단계와 같은 접근(2026-08-28 결정).

CREATE TABLE review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE review_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_review_comment_review (review_id),
    CONSTRAINT fk_review_comment_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO review (account_id, title, body, status, created_at, updated_at) VALUES
  (1, '잊지 못할 무대였습니다', '음식과 공연이 이렇게 자연스럽게 어우러질 수 있다는 걸 처음 알았어요. 다음 시즌도 꼭 예약하려고요.', 'PUBLISHED', NOW(6), NOW(6)),
  (2, '친구와 함께 다녀왔어요', '코스 사이사이 장면 전환이 인상 깊었습니다. 좌석에 따라 보이는 각도가 달라서 다음엔 다른 자리로 예약해보고 싶어요.', 'PUBLISHED', NOW(6), NOW(6)),
  (3, '스팸성 홍보 문구', '여기 클릭하면 할인쿠폰 드려요 http://example.com', 'HIDDEN', NOW(6), NOW(6));

INSERT INTO review_comment (review_id, account_id, body, status, created_at, updated_at) VALUES
  (1, 4, '저도 다녀왔는데 정말 좋았어요! 어느 코스가 제일 기억에 남으세요?', 'ACTIVE', NOW(6), NOW(6));
