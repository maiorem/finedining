package com.finediningtheater.artist;

import com.finediningtheater.artist.dto.CastingAdminResponse;
import com.finediningtheater.artist.dto.UpsertCastingTranslationRequest;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
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

/** 모집 공고 편집(기능6). 발행/발행취소는 sudo 모드를 요구한다(§3.4). */
@RestController
@RequestMapping("/api/castings")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class CastingEditController {

    private final CastingService castingService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<CastingAdminResponse>> listForAdmin() {
        List<CastingAdminResponse> body =
                castingService.listForAdmin().stream().map(CastingAdminResponse::from).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<CastingAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(CastingAdminResponse.from(castingService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<CastingAdminResponse> create(
            @AuthenticationPrincipal AdminPrincipal principal, HttpServletRequest httpRequest) {
        Casting casting = castingService.create();

        auditLogger.record(
                principal.id(), "CASTING_CREATE", "Casting", casting.getId(), null, null, ClientIp.resolve(httpRequest));

        return ApiResponse.success(CastingAdminResponse.from(casting));
    }

    @PutMapping("/{id}/translations/{locale}")
    public ApiResponse<CastingAdminResponse> saveDraftTranslation(
            @PathVariable Long id,
            @PathVariable SiteLocale locale,
            @Valid @RequestBody UpsertCastingTranslationRequest request) {
        castingService.saveDraftTranslation(id, locale, request.title(), request.body());
        return ApiResponse.success(CastingAdminResponse.from(castingService.getForAdmin(id)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CastingAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = castingService.getForAdmin(id).getStatus().name();
        Casting after = castingService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "CASTING_PUBLISH",
                "Casting",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(CastingAdminResponse.from(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<CastingAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = castingService.getForAdmin(id).getStatus().name();
        Casting after = castingService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "CASTING_UNPUBLISH",
                "Casting",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(CastingAdminResponse.from(after));
    }
}
