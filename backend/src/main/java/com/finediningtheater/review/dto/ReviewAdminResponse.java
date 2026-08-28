package com.finediningtheater.review.dto;

import com.finediningtheater.review.Review;
import java.time.Instant;
import java.util.List;

public record ReviewAdminResponse(
        Long id,
        String title,
        String body,
        Long accountId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<ReviewCommentResponse> comments) {

    public static ReviewAdminResponse from(Review review, List<ReviewCommentResponse> comments) {
        return new ReviewAdminResponse(
                review.getId(),
                review.getTitle(),
                review.getBody(),
                review.getAccountId(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                comments);
    }
}
