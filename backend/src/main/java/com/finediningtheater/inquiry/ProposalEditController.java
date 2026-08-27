package com.finediningtheater.inquiry;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.inquiry.dto.ProposalAdminResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 협업 제안 검토 — 관리자로 로그인해야 목록을 볼 수 있다(§3.7). 별도 어드민을 만들지 않고
 * 편집 모드 안에 둔다(§3.5). PIN sudo는 요구하지 않는다 — 수락/거절은 §3.4의 파괴적·공개적
 * 동작 목록에 없다.
 */
@RestController
@RequestMapping("/api/proposals")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ProposalEditController {

    private final ProposalService proposalService;
    private final AuditLogger auditLogger;

    @GetMapping("/manage")
    public ApiResponse<List<ProposalAdminResponse>> listForAdmin() {
        List<ProposalAdminResponse> body =
                proposalService.listForAdmin().stream().map(ProposalAdminResponse::from).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ProposalAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(ProposalAdminResponse.from(proposalService.getForAdmin(id)));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ProposalAdminResponse> accept(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = proposalService.getForAdmin(id).getStatus().name();
        Proposal after = proposalService.accept(id);

        auditLogger.record(
                principal.id(),
                "PROPOSAL_ACCEPT",
                "Proposal",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProposalAdminResponse.from(after));
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<ProposalAdminResponse> decline(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = proposalService.getForAdmin(id).getStatus().name();
        Proposal after = proposalService.decline(id);

        auditLogger.record(
                principal.id(),
                "PROPOSAL_DECLINE",
                "Proposal",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ProposalAdminResponse.from(after));
    }
}
