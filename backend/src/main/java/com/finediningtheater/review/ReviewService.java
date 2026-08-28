package com.finediningtheater.review;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리뷰 공개 조회 + 관리자 모더레이션(2026-08-28, 우선순위 5단계). 작성·본인 수정·삭제는 아직
 * 없다 — 로그인 회원(카카오)이 없어 작성자 신원을 확인할 방법이 없기 때문이다(§3.6). 그 경로는
 * 카카오 로그인이 붙는 마지막 단계에서 함께 연다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

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
        review.adminEditContent(title, body);
        return review;
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
