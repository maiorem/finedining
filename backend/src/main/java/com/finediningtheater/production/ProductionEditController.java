package com.finediningtheater.production;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.production.dto.CreateProductionRequest;
import com.finediningtheater.production.dto.ProductionAdminResponse;
import com.finediningtheater.production.dto.UpsertTranslationRequest;
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
 * 작품 편집(2순위: 작품 아카이빙) + 이미지 목록 조회. 이미지 자체의 업로드·삭제는
 * {@code MediaEditController}가 맡는다. 발행/발행취소는 파괴적·공개적 동작이라 sudo 모드를
 * 요구한다(CLAUDE.md §3.4). 모든 쓰기에 감사 로그를 남긴다(§7.7).
 */
@RestController
@RequestMapping("/api/productions")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ProductionEditController {

    private final ProductionService productionService;
    private final MediaService mediaService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<ProductionAdminResponse>> listForAdmin() {
        List<ProductionAdminResponse> body =
                productionService.listForAdmin().stream().map(this::toAdminResponse).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ProductionAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(toAdminResponse(productionService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<ProductionAdminResponse> create(
            @Valid @RequestBody CreateProductionRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        Production production = productionService.create(request.slug());

        auditLogger.record(
                principal.id(),
                "PRODUCTION_CREATE",
                "Production",
                production.getId(),
                null,
                Map.of("slug", request.slug()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(production));
    }

    /** "임시저장" — 공개본에는 영향이 없다(§3.9). */
    @PutMapping("/{id}/translations/{locale}")
    public ApiResponse<ProductionAdminResponse> saveDraftTranslation(
            @PathVariable Long id,
            @PathVariable SiteLocale locale,
            @Valid @RequestBody UpsertTranslationRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        ProductionTranslation beforeRow = productionService.getForAdmin(id).translationRowFor(locale);
        Map<String, String> before =
                beforeRow == null
                        ? null
                        : Map.of(
                                "draftTitle", String.valueOf(beforeRow.getDraftTitle()),
                                "draftSubtitle", String.valueOf(beforeRow.getDraftSubtitle()));

        productionService.saveDraftTranslation(id, locale, request.title(), request.subtitle());

        auditLogger.record(
                principal.id(),
                "PRODUCTION_SAVE_DRAFT",
                "Production",
                id,
                before,
                Map.of("draftTitle", request.title(), "draftSubtitle", String.valueOf(request.subtitle())),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(productionService.getForAdmin(id)));
    }

    /** 파괴적·공개적 동작 — PIN sudo 모드가 열려 있어야 한다(§3.4). */
    @PostMapping("/{id}/publish")
    public ApiResponse<ProductionAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = productionService.getForAdmin(id).getStatus().name();
        Production after = productionService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "PRODUCTION_PUBLISH",
                "Production",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ProductionAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = productionService.getForAdmin(id).getStatus().name();
        Production after = productionService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "PRODUCTION_UNPUBLISH",
                "Production",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(toAdminResponse(after));
    }

    // 관리자는 PENDING·FAILED 이미지도 봐야 재시도·삭제할 수 있으므로 listForAdmin(전체)을 쓴다.
    private ProductionAdminResponse toAdminResponse(Production production) {
        List<MediaAssetResponse> images =
                mediaService.listForAdmin(production.getId()).stream()
                        .map(asset -> MediaAssetResponse.from(asset, mediaService))
                        .toList();
        return ProductionAdminResponse.from(production, images);
    }
}
