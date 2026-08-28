package com.finediningtheater.about;

import com.finediningtheater.about.dto.AboutAdminResponse;
import com.finediningtheater.about.dto.UpsertAboutTranslationRequest;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
 * 소개문 편집(§1·§6). 발행/발행취소는 파괴적·공개적 동작이라 sudo 모드를 요구한다(§3.4).
 * 생성 엔드포인트가 없다 — 싱글턴이라 Flyway가 심은 행 하나만 계속 편집한다.
 */
@RestController
@RequestMapping("/api/about")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class AboutEditController {

    private final AboutService aboutService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<AboutAdminResponse> getForAdmin() {
        return ApiResponse.success(AboutAdminResponse.from(aboutService.getForAdmin()));
    }

    /** "임시저장" — 공개본에는 영향이 없다(§3.9). */
    @PutMapping("/translations/{locale}")
    public ApiResponse<AboutAdminResponse> saveDraftTranslation(
            @PathVariable SiteLocale locale, @Valid @RequestBody UpsertAboutTranslationRequest request) {
        aboutService.saveDraftTranslation(locale, request.intro());
        return ApiResponse.success(AboutAdminResponse.from(aboutService.getForAdmin()));
    }

    /** 파괴적·공개적 동작 — PIN sudo 모드가 열려 있어야 한다(§3.4). */
    @PostMapping("/publish")
    public ApiResponse<AboutAdminResponse> publish(
            @AuthenticationPrincipal AdminPrincipal principal, HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = aboutService.getForAdmin().getStatus().name();
        AboutContent after = aboutService.publish(principal.id());

        auditLogger.record(
                principal.id(),
                "ABOUT_PUBLISH",
                "AboutContent",
                after.getId(),
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(AboutAdminResponse.from(after));
    }

    @PostMapping("/unpublish")
    public ApiResponse<AboutAdminResponse> unpublish(
            @AuthenticationPrincipal AdminPrincipal principal, HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = aboutService.getForAdmin().getStatus().name();
        AboutContent after = aboutService.unpublish();

        auditLogger.record(
                principal.id(),
                "ABOUT_UNPUBLISH",
                "AboutContent",
                after.getId(),
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(AboutAdminResponse.from(after));
    }
}
