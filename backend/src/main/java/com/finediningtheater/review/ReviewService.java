package com.finediningtheater.review;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리뷰 공개 조회 + 관리자 모더레이션 + 회원 본인 글 작성·수정·삭제(2026-09-03, 카카오 로그인이
 * 붙은 뒤 §3.6의 작성 경로를 연다). 소유권 검사는 여기서 한다 — {@code @PreAuthorize}가 아니라
 * 서비스 계층의 책임이다(§3.3). 관리자는 이 검사를 우회할 수 있는 유일한 경로다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    // CLAUDE.md §3.6 "최소 구성" — 스팸의 90%는 이 두 제한으로 막힌다.
    private static final int DAILY_POST_LIMIT = 10;
    private static final Duration MIN_POST_INTERVAL = Duration.ofSeconds(30);

    // 리치 에디터를 쓰지 않는다 — 플레인 텍스트만 허용하고 HTML 입력은 거부한다(§3.6).
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;

    public List<Review> listPublished() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PUBLISHED);
    }

    public Review getPublished(Long id) {
        Review review = findOrThrow(id);
        if (!review.isPublished()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }
        return review;
    }

    public List<ReviewComment> listActiveComments(Long reviewId) {
        return reviewCommentRepository.findByReviewIdAndStatusOrderByCreatedAtAsc(reviewId, ReviewCommentStatus.ACTIVE);
    }

    public List<Review> listForAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public Review getForAdmin(Long id) {
        return findOrThrow(id);
    }

    @Transactional
    public Review hide(Long id) {
        Review review = findOrThrow(id);
        if (!review.isPublished()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        review.hide();
        return review;
    }

    @Transactional
    public Review restore(Long id) {
        Review review = findOrThrow(id);
        if (!review.isHidden()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        review.restore();
        return review;
    }

    @Transactional
    public Review softDelete(Long id) {
        Review review = findOrThrow(id);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        review.softDelete();
        return review;
    }

    @Transactional
    public Review adminEditContent(Long id, String title, String body) {
        Review review = findOrThrow(id);
        review.editContent(title, body);
        return review;
    }

    /** 회원 본인 작성. 하루 상한·연속 등록 간격은 §3.6, 새니타이즈는 아래 sanitize() 참고. */
    @Transactional
    public Review create(Long accountId, String title, String body) {
        enforcePostingLimits(accountId);
        Review review = new Review(accountId, sanitize(title), sanitize(body));
        return reviewRepository.save(review);
    }

    /** 본인 글만 수정할 수 있다 — 아니면 POST_NOT_OWNED(§3.3). 삭제된 글은 수정 대상이 아니다. */
    @Transactional
    public Review editOwnContent(Long id, Long accountId, String title, String body) {
        Review review = findOrThrow(id);
        requireOwnership(review, accountId);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        review.editContent(sanitize(title), sanitize(body));
        return review;
    }

    /** 본인 글만 삭제할 수 있다 — 아니면 POST_NOT_OWNED(§3.3). */
    @Transactional
    public Review softDeleteOwn(Long id, Long accountId) {
        Review review = findOrThrow(id);
        requireOwnership(review, accountId);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        review.softDelete();
        return review;
    }

    private void requireOwnership(Review review, Long accountId) {
        if (!review.getAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.POST_NOT_OWNED);
        }
    }

    private void enforcePostingLimits(Long accountId) {
        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
        if (reviewRepository.countByAccountIdAndCreatedAtAfter(accountId, since) >= DAILY_POST_LIMIT) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        reviewRepository
                .findTopByAccountIdOrderByCreatedAtDesc(accountId)
                .ifPresent(
                        last -> {
                            if (Duration.between(last.getCreatedAt(), Instant.now()).compareTo(MIN_POST_INTERVAL) < 0) {
                                throw new BusinessException(ErrorCode.RATE_LIMITED);
                            }
                        });
    }

    private String sanitize(String text) {
        String trimmed = text.trim();
        if (HTML_TAG_PATTERN.matcher(trimmed).find()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "본문에는 HTML 태그를 쓸 수 없습니다.");
        }
        return trimmed;
    }

    @Transactional
    public void softDeleteComment(Long commentId) {
        ReviewComment comment =
                reviewCommentRepository
                        .findById(commentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        comment.softDelete();
    }

    private Review findOrThrow(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
