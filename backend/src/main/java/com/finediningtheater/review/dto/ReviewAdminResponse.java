package com.finediningtheater.review.dto;

import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.review.Review;
import java.time.Instant;
import java.util.List;

public record ReviewAdminResponse(
        Long id,
        String title,
        String body,
        Long accountId,
        String authorName,
        String contact,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<ReviewCommentResponse> comments,
        List<MediaAssetResponse> images) {

    public static ReviewAdminResponse from(
            Review review, List<ReviewCommentResponse> comments, List<MediaAssetResponse> images) {
        return new ReviewAdminResponse(
                review.getId(),
                review.getTitle(),
                review.getBody(),
                review.getAccountId(),
                review.getAuthorName(),
                review.getContact(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                comments,
                images);
    }
}
