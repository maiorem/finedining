package com.finediningtheater.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewCommentRepository reviewCommentRepository;

    private ReviewService service() {
        return new ReviewService(reviewRepository, reviewCommentRepository);
    }

    @Test
    void 목록_조회는_PUBLISHED_상태만_요청한다() {
        when(reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PUBLISHED))
                .thenReturn(List.of(new Review(1L, "제목", "본문")));

        List<Review> result = service().listPublished();

        assertThat(result).hasSize(1);
    }

    @Test
    void HIDDEN_리뷰는_공개_조회에서_404다() {
        Review review = new Review(1L, "제목", "본문");
        review.hide();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service().getPublished(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    void PUBLISHED_상태만_숨길_수_있다() {
        Review review = new Review(1L, "제목", "본문");
        review.hide();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service().hide(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    @Test
    void 숨긴_리뷰를_복구하면_PUBLISHED로_돌아간다() {
        Review review = new Review(1L, "제목", "본문");
        review.hide();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        Review result = service().restore(1L);

        assertThat(result.isPublished()).isTrue();
    }

    @Test
    void PUBLISHED_상태가_아니면_복구를_거부한다() {
        Review review = new Review(1L, "제목", "본문");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service().restore(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    @Test
    void 이미_삭제된_리뷰는_다시_삭제할_수_없다() {
        Review review = new Review(1L, "제목", "본문");
        review.softDelete();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service().softDelete(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    @Test
    void 물리_삭제하지_않고_상태만_DELETED로_바꾼다() {
        Review review = new Review(1L, "제목", "본문");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        Review result = service().softDelete(1L);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    @Test
    void 관리자는_원문을_수정할_수_있다() {
        Review review = new Review(1L, "원래 제목", "원래 본문");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        Review result = service().adminEditContent(1L, "새 제목", "새 본문");

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getBody()).isEqualTo("새 본문");
    }
}
