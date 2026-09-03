package com.finediningtheater.review;

import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.review.dto.ReviewCommentResponse;
import com.finediningtheater.review.dto.ReviewDetailResponse;
import com.finediningtheater.review.dto.ReviewSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 조회 전용. 작성·수정·삭제는 ReviewEditController가 맡는다(§3.6). */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<List<ReviewSummaryResponse>> list() {
        List<ReviewSummaryResponse> body =
                reviewService.listPublished().stream().map(ReviewSummaryResponse::from).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewDetailResponse> detail(@PathVariable Long id) {
        Review review = reviewService.getPublished(id);
        List<ReviewCommentResponse> comments =
                reviewService.listActiveComments(id).stream().map(ReviewCommentResponse::from).toList();
        return ApiResponse.success(ReviewDetailResponse.from(review, comments));
    }
}
