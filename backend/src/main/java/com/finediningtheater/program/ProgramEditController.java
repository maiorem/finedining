package com.finediningtheater.program;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.program.dto.ChangeProgramLinkRequest;
import com.finediningtheater.program.dto.ProgramAdminResponse;
import com.finediningtheater.program.dto.UpsertProgramTranslationRequest;
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
 * 프로그램 편집. Casting/Artist와 같은 패턴이다 — 발행/발행취소는 sudo 모드를 요구한다(§3.4).
 * 모든 쓰기에 감사 로그를 남긴다(§7.7).
 */
@RestController
@RequestMapping("/api/programs")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ProgramEditController {

    private final ProgramService programService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<ProgramAdminResponse>> listForAdmin() {
        List<ProgramAdminResponse> body =
                programService.listForAdmin().stream().map(ProgramAdminResponse::from).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ProgramAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(ProgramAdminResponse.from(programService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<ProgramAdminResponse> create(
            @AuthenticationPrincipal AdminPrincipal principal, HttpServletRequest httpRequest) {
        Program program = programService.create();

        auditLogger.record(
                principal.id(), "PROGRAM_CREATE", "Program", program.getId(), null, null, ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProgramAdminResponse.from(program));
    }

    /** "임시저장" — 공개본에는 영향이 없다(§3.9). */
    @PutMapping("/{id}/translations/{locale}")
    public ApiResponse<ProgramAdminResponse> saveDraftTranslation(
            @PathVariable Long id,
            @PathVariable SiteLocale locale,
            @Valid @RequestBody UpsertProgramTranslationRequest request) {
        programService.saveDraftTranslation(id, locale, request.title(), request.description());
        return ApiResponse.success(ProgramAdminResponse.from(programService.getForAdmin(id)));
    }

    /** 참가 링크(구글폼) 변경 — Artist.linkUrl과 같은 취급이라 sudo 불필요. */
    @PutMapping("/{id}/apply-url")
    public ApiResponse<ProgramAdminResponse> changeApplyUrl(
            @PathVariable Long id,
            @Valid @RequestBody ChangeProgramLinkRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeUrl = String.valueOf(programService.getForAdmin(id).getApplyUrl());
        Program after = programService.changeApplyUrl(id, request.url());

        auditLogger.record(
                principal.id(),
                "PROGRAM_APPLY_URL_CHANGE",
                "Program",
                id,
                Map.of("applyUrl", beforeUrl),
                Map.of("applyUrl", String.valueOf(after.getApplyUrl())),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProgramAdminResponse.from(after));
    }

    @PutMapping("/{id}/location-url")
    public ApiResponse<ProgramAdminResponse> changeLocationUrl(
            @PathVariable Long id,
            @Valid @RequestBody ChangeProgramLinkRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeUrl = String.valueOf(programService.getForAdmin(id).getLocationUrl());
        Program after = programService.changeLocationUrl(id, request.url());

        auditLogger.record(
                principal.id(),
                "PROGRAM_LOCATION_URL_CHANGE",
                "Program",
                id,
                Map.of("locationUrl", beforeUrl),
                Map.of("locationUrl", String.valueOf(after.getLocationUrl())),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProgramAdminResponse.from(after));
    }

    /** 파괴적·공개적 동작 — PIN sudo 모드가 열려 있어야 한다(§3.4). */
    @PostMapping("/{id}/publish")
    public ApiResponse<ProgramAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = programService.getForAdmin(id).getStatus().name();
        Program after = programService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "PROGRAM_PUBLISH",
                "Program",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProgramAdminResponse.from(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ProgramAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = programService.getForAdmin(id).getStatus().name();
        Program after = programService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "PROGRAM_UNPUBLISH",
                "Program",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProgramAdminResponse.from(after));
    }
}
