package com.finediningtheater.review.dto;

import com.finediningtheater.review.ReviewComment;
import java.time.Instant;

public record ReviewCommentResponse(Long id, Long accountId, String body, Instant createdAt) {

    public static ReviewCommentResponse from(ReviewComment comment) {
        return new ReviewCommentResponse(comment.getId(), comment.getAccountId(), comment.getBody(), comment.getCreatedAt());
    }
}
