package com.finediningtheater.site;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.site.dto.SiteStatusResponse;
import com.finediningtheater.site.dto.SiteVisibilityRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사이트 공개/비공개 전환(2026-09-19). 공개는 사이트 전체를 외부에 여는 가장 공개적인 동작이라
 * SUPER_ADMIN만 할 수 있고 PIN 확인(sudo 모드)을 요구한다(§3.4).
 */
@RestController
@RequestMapping("/api/site")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SiteEditController {

    // audit_log.target_id가 NOT NULL이라 설정 행이 하나뿐인 이 대상에는 고정값을 쓴다.
    private static final long AUDIT_TARGET_ID = 1L;

    private final SiteVisibilityService siteVisibilityService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @PutMapping("/visibility")
    public ApiResponse<SiteStatusResponse> changeVisibility(
            @Valid @RequestBody SiteVisibilityRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        boolean before = siteVisibilityService.isPublic();
        siteVisibilityService.setPublic(request.open());

        auditLogger.record(
                principal.id(),
                "SITE_VISIBILITY_CHANGE",
                "SiteSetting",
                AUDIT_TARGET_ID,
                Map.of("open", before),
                Map.of("open", request.open()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(new SiteStatusResponse(request.open()));
    }
}
