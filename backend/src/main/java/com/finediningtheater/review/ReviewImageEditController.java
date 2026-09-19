package com.finediningtheater.review;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.MemberPrincipal;
import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.media.dto.PresignResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원이 자기 이야기에 이미지를 올리는 경로(2026-09-19). 관리자용 /api/media/presign과 달리 회원
 * 세션만 받고, 글 소유권·3장 상한·10MB·시간당 10회를 서버가 강제한다. 클래스 레벨은 다른
 * *EditController와 같이 EDITOR로 잠그고, 메서드에서 isAuthenticated()로 회원에게 연다(ReviewEditController와 같은 패턴).
 */
@RestController
@RequestMapping("/api/reviews/{reviewId}/images")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ReviewImageEditController {

    private final ReviewImageService reviewImageService;
    private final MediaService mediaService;

    public record ReviewPresignRequest(@NotBlank String contentType, @Positive long contentLengthBytes) {}

    @PostMapping("/presign")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PresignResponse> presign(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewPresignRequest request,
            @AuthenticationPrincipal MemberPrincipal member) {
        MediaService.PresignResult result =
                reviewImageService.presign(reviewId, requireMember(member).id(), request.contentType(), request.contentLengthBytes());
        return ApiResponse.success(new PresignResponse(result.mediaAssetId(), result.uploadUrl()));
    }

    @PostMapping("/{mediaId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MediaAssetResponse> complete(
            @PathVariable Long reviewId, @PathVariable Long mediaId, @AuthenticationPrincipal MemberPrincipal member) {
        MediaAsset asset = reviewImageService.complete(reviewId, requireMember(member).id(), mediaId);
        return ApiResponse.success(MediaAssetResponse.from(asset, mediaService));
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(
            @PathVariable Long reviewId, @PathVariable Long mediaId, @AuthenticationPrincipal MemberPrincipal member) {
        reviewImageService.delete(reviewId, requireMember(member).id(), mediaId);
        return ApiResponse.ok();
    }

    // 관리자 세션은 여기서 거부한다 — 관리자는 /api/media/*로 이미지를 관리한다.
    private MemberPrincipal requireMember(MemberPrincipal member) {
        if (member == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }
}
