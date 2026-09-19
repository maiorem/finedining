package com.finediningtheater.account;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.security.MemberPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 본인 탈퇴. 관리자 세션은 받지 않는다 — 관리자 계정은 탈퇴 개념이 없다(§3.1). 탈퇴하면
 * refresh 쿠키를 지우고, 이후 refresh는 계정 상태 검사(MemberAuthService)에서 거부된다.
 */
@RestController
@RequestMapping("/api/auth/member")
@RequiredArgsConstructor
public class MemberWithdrawalController {

    private final MemberWithdrawalService memberWithdrawalService;
    private final AuditLogger auditLogger;

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal MemberPrincipal member,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (member == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        memberWithdrawalService.withdraw(member.id());
        MemberRefreshCookie.clear(httpResponse);

        auditLogger.record(
                member.id(),
                "MEMBER_WITHDRAW",
                "Account",
                member.id(),
                Map.of("status", "ACTIVE"),
                Map.of("status", "WITHDRAWN"),
                ClientIp.resolve(httpRequest));

        return ApiResponse.ok();
    }
}
