package com.finediningtheater.review;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 공개 조회는 항상 PUBLISHED로 필터한다 (CLAUDE.md §7.3).
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    List<Review> findAllByOrderByCreatedAtDesc();

    // 회원당 하루 게시글 상한 검사용 (CLAUDE.md §3.6).
    // 탈퇴 시 이름·연락처만 지운다 — 글 자체는 그대로 남긴다(§3.2).
    @Modifying
    @Query("update Review r set r.authorName = null, r.contact = null where r.accountId = :accountId")
    int clearAuthorInfoByAccountId(@Param("accountId") Long accountId);

    long countByAccountIdAndCreatedAtAfter(Long accountId, Instant since);

    // 연속 등록 간격 제한(30초) 검사용 (CLAUDE.md §3.6).
    Optional<Review> findTopByAccountIdOrderByCreatedAtDesc(Long accountId);
}
