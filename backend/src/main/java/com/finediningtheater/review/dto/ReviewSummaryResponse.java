package com.finediningtheater.review.dto;

import com.finediningtheater.review.Review;
import java.time.Instant;

public record ReviewSummaryResponse(Long id, String title, Long accountId, Instant createdAt) {

    public static ReviewSummaryResponse from(Review review) {
        return new ReviewSummaryResponse(review.getId(), review.getTitle(), review.getAccountId(), review.getCreatedAt());
    }
}
