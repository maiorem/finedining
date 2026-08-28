package com.finediningtheater.review.dto;

import com.finediningtheater.review.Review;
import java.time.Instant;
import java.util.List;

public record ReviewDetailResponse(
        Long id, String title, String body, Long accountId, Instant createdAt, List<ReviewCommentResponse> comments) {

    public static ReviewDetailResponse from(Review review, List<ReviewCommentResponse> comments) {
        return new ReviewDetailResponse(
                review.getId(), review.getTitle(), review.getBody(), review.getAccountId(), review.getCreatedAt(), comments);
    }
}
