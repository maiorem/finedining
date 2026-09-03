package com.finediningtheater.media;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.media.dto.CompleteUploadRequest;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.media.dto.PresignRequest;
import com.finediningtheater.media.dto.PresignResponse;
import com.finediningtheater.media.dto.UpdateAltTextRequest;
import com.finediningtheater.media.dto.UpdateCaptionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지 업로드 파이프라인 (CLAUDE.md §7.5). presign 발급 자체가 이 시스템에서 가장 위험한
 * API이므로(§3.5) 전부 관리자 전용이다. PIN sudo 모드는 요구하지 않는다 — 업로드는 발행 전
 * 초안 단계 작업이고, 공개 노출은 Production.publish()가 별도로 통제한다.
 */
@RestController
@RequestMapping("/api/media")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class MediaEditController {

    private final MediaService mediaService;
    private final AuditLogger auditLogger;

    @PostMapping("/presign")
    public ApiResponse<PresignResponse> presign(
            @Valid @RequestBody PresignRequest request, @AuthenticationPrincipal AdminPrincipal principal) {
        MediaService.PresignResult result =
                mediaService.presign(
                        request.ownerType(),
                        request.ownerId(),
                        principal.id(),
                        request.contentType(),
                        request.contentLengthBytes());
        return ApiResponse.success(new PresignResponse(result.mediaAssetId(), result.uploadUrl()));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<MediaAssetResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteUploadRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        MediaAsset asset = mediaService.completeUpload(id, request.altText());

        auditLogger.record(
                principal.id(),
                "MEDIA_UPLOAD_COMPLETE",
                "MediaAsset",
                id,
                null,
                Map.of("status", asset.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(MediaAssetResponse.from(asset, mediaService));
    }

    /** 이미 업로드가 끝난 이미지의 캡션(대체 텍스트)만 고친다 — 다시 올리지 않아도 된다. */
    @PutMapping("/{id}")
    public ApiResponse<MediaAssetResponse> updateAltText(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAltTextRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeAltText = mediaService.get(id).getAltText();

        MediaAsset asset = mediaService.updateAltText(id, request.altText());

        auditLogger.record(
                principal.id(),
                "MEDIA_ALT_TEXT_UPDATE",
                "MediaAsset",
                id,
                beforeAltText,
                asset.getAltText(),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(MediaAssetResponse.from(asset, mediaService));
    }

    /** 방문자에게 보이는 설명 문단을 고친다 — altText와 별개 경로다(2026-09-04, §8.2). */
    @PutMapping("/{id}/caption")
    public ApiResponse<MediaAssetResponse> updateCaption(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCaptionRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeCaption = mediaService.get(id).getCaption();

        MediaAsset asset = mediaService.updateCaption(id, request.caption());

        auditLogger.record(
                principal.id(),
                "MEDIA_CAPTION_UPDATE",
                "MediaAsset",
                id,
                beforeCaption,
                asset.getCaption(),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(MediaAssetResponse.from(asset, mediaService));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal, HttpServletRequest httpRequest) {
        mediaService.delete(id);

        auditLogger.record(
                principal.id(), "MEDIA_DELETE", "MediaAsset", id, null, null, ClientIp.resolve(httpRequest));

        return ApiResponse.ok();
    }
}
