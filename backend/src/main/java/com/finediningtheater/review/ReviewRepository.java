package com.finediningtheater.review;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 공개 조회는 항상 PUBLISHED로 필터한다 (CLAUDE.md §7.3).
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<Review> findAllByOrderByCreatedAtDesc();

    // 회원당 하루 게시글 상한 검사용 (CLAUDE.md §3.6).
    long countByAccountIdAndCreatedAtAfter(Long accountId, Instant since);

    // 연속 등록 간격 제한(30초) 검사용 (CLAUDE.md §3.6).
    Optional<Review> findTopByAccountIdOrderByCreatedAtDesc(Long accountId);
}
