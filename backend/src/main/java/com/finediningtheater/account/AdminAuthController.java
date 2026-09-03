package com.finediningtheater.account;

import com.finediningtheater.account.dto.AdminLoginRequest;
import com.finediningtheater.account.dto.AdminLoginResponse;
import com.finediningtheater.account.dto.ChangePasswordRequest;
import com.finediningtheater.account.dto.SetPinRequest;
import com.finediningtheater.account.dto.VerifyPinRequest;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.support.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 로그인. 회원가입 절차가 없고, 카카오 OAuth2와도 무관한 별개 흐름이다
 * (CLAUDE.md §3.1·§7.4). 로그인 자체는 공개 엔드포인트이므로 IP당 레이트리밋을 건다(§3.5).
 */
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private static final String REFRESH_COOKIE_NAME = "admin_refresh_token";
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(14).toSeconds();
    private static final int LOGIN_LIMIT_PER_MINUTE = 10;

    private final AdminAuthService adminAuthService;
    private final RateLimiter rateLimiter;

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ip = ClientIp.resolve(httpRequest);
        if (!rateLimiter.tryAcquire("admin-login:" + ip, LOGIN_LIMIT_PER_MINUTE)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }

        AdminSession session = adminAuthService.login(request.username(), request.password());
        setRefreshCookie(httpResponse, session.refreshToken());
        return ApiResponse.success(toResponse(session));
    }

    @PostMapping("/refresh")
    public ApiResponse<AdminLoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AdminSession session = adminAuthService.refresh(refreshToken);
        setRefreshCookie(httpResponse, session.refreshToken());
        return ApiResponse.success(toResponse(session));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse httpResponse) {
        clearRefreshCookie(httpResponse);
        return ApiResponse.ok();
    }

    /** PIN 최초 설정/변경. sudo 모드가 아니라 현재 비밀번호 재확인으로 본인을 증명한다 — PIN이
     * 아직 없는 상태에서 sudo를 요구하면 닭-달걀 문제가 된다. 인증은 필요하다(§3.4). */
    @PostMapping("/pin")
    public ApiResponse<Void> setPin(
            @AuthenticationPrincipal AdminPrincipal principal, @Valid @RequestBody SetPinRequest request) {
        adminAuthService.setPin(principal.id(), request.currentPassword(), request.newPin());
        return ApiResponse.ok();
    }

    /** 로그인 비밀번호 변경. 시드 비밀번호를 영구히 쓰지 않게 하는 통로다(§3.1). PIN 변경과
     * 동일하게 sudo가 아니라 현재 비밀번호 재확인으로 본인을 증명한다. */
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AdminPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
        adminAuthService.changePassword(principal.id(), request.currentPassword(), request.newPassword());
        return ApiResponse.ok();
    }

    /** PIN 확인 → 통과하면 15분짜리 sudo 모드가 열린다(§3.4). */
    @PostMapping("/sudo")
    public ApiResponse<Void> verifySudo(
            @AuthenticationPrincipal AdminPrincipal principal, @Valid @RequestBody VerifyPinRequest request) {
        adminAuthService.verifySudo(principal.id(), request.pin());
        return ApiResponse.ok();
    }

    private AdminLoginResponse toResponse(AdminSession session) {
        return new AdminLoginResponse(session.accessToken(), session.username(), session.role().name());
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie =
                ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path("/api/auth/admin")
                        .maxAge(REFRESH_COOKIE_MAX_AGE_SECONDS)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie =
                ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path("/api/auth/admin")
                        .maxAge(0)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
