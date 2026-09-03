package com.finediningtheater.press;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.press.dto.PressClippingAdminResponse;
import com.finediningtheater.press.dto.PressClippingContentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보도자료 편집(소개 페이지 탭, 2026-09-04). 제목·링크는 draft 없이 즉시 반영된다(Artist의
 * linkUrl과 같은 취급). 발행/발행취소는 파괴적·공개적 동작이라 sudo 모드를 요구한다(§3.4).
 */
@RestController
@RequestMapping("/api/press-clippings")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class PressClippingEditController {

    private final PressClippingService pressClippingService;
    private final MediaService mediaService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<PressClippingAdminResponse>> listForAdmin() {
        // Artist/Program 목록과 달리 여기서는 항목마다 이미지를 함께 불러온다 — 상세 페이지가
        // 없어(슬러그 없음) 관리 화면이 목록 하나로 인라인 편집을 끝내야 하고, 보도자료는 항목
        // 수 자체가 적어(개별 이미지 1장씩) N+1 비용이 무시할 만하다.
        List<PressClippingAdminResponse> body =
                pressClippingService.listForAdmin().stream().map(this::toAdminResponse).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<PressClippingAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(toAdminResponse(pressClippingService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<PressClippingAdminResponse> create(
            @Valid @RequestBody PressClippingContentRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        PressClipping clipping = pressClippingService.create(request.title(), request.externalUrl());

        auditLogger.record(
                principal.id(),
                "PRESS_CLIPPING_CREATE",
                "PressClipping",
                clipping.getId(),
                null,
                Map.of("title", clipping.getTitle(), "externalUrl", clipping.getExternalUrl()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(clipping));
    }

    @PutMapping("/{id}")
    public ApiResponse<PressClippingAdminResponse> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody PressClippingContentRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        PressClipping before = pressClippingService.getForAdmin(id);
        Map<String, String> beforeSnapshot = Map.of("title", before.getTitle(), "externalUrl", before.getExternalUrl());

        PressClipping after = pressClippingService.updateContent(id, request.title(), request.externalUrl());

        auditLogger.record(
                principal.id(),
                "PRESS_CLIPPING_UPDATE",
                "PressClipping",
                id,
                beforeSnapshot,
                Map.of("title", after.getTitle(), "externalUrl", after.getExternalUrl()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<PressClippingAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = pressClippingService.getForAdmin(id).getStatus().name();
        PressClipping after = pressClippingService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "PRESS_CLIPPING_PUBLISH",
                "PressClipping",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<PressClippingAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = pressClippingService.getForAdmin(id).getStatus().name();
        PressClipping after = pressClippingService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "PRESS_CLIPPING_UNPUBLISH",
                "PressClipping",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    private PressClippingAdminResponse toAdminResponse(PressClipping clipping) {
        List<MediaAssetResponse> images =
                mediaService.listForAdmin(MediaOwnerType.PRESS_CLIPPING, clipping.getId()).stream()
                        .map(asset -> MediaAssetResponse.from(asset, mediaService))
                        .toList();
        return PressClippingAdminResponse.from(clipping, images);
    }
}
