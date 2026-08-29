package com.finediningtheater.account;

import com.finediningtheater.account.dto.MemberSessionResponse;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일반 회원 세션 재발급·로그아웃. 로그인 자체는 여기 없다 — 카카오 OAuth2 리다이렉트 흐름
 * (/api/oauth2/authorization/kakao)이 로그인이고, 성공하면 OAuth2LoginSuccessHandler가 이미
 * 세션을 발급해 리다이렉트로 돌려준다(CLAUDE.md §7.4).
 */
@RestController
@RequestMapping("/api/auth/member")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    @PostMapping("/refresh")
    public ApiResponse<MemberSessionResponse> refresh(
            @CookieValue(name = MemberRefreshCookie.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        MemberSession session = memberAuthService.refresh(refreshToken);
        MemberRefreshCookie.set(httpResponse, session.refreshToken());
        return ApiResponse.success(new MemberSessionResponse(session.accessToken(), session.nickname()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse httpResponse) {
        MemberRefreshCookie.clear(httpResponse);
        return ApiResponse.ok();
    }
}
