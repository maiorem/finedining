package com.finediningtheater.account;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 회원 refresh 쿠키 설정을 한 곳에 모은다 — OAuth2 로그인 성공 핸들러와 /api/auth/member/refresh
 * 양쪽에서 같은 이름·경로로 쓴다(CLAUDE.md §7.4). access token은 응답 본문으로만 준다(§7.4).
 */
public final class MemberRefreshCookie {

    // @CookieValue(name = ...)는 컴파일타임 상수만 받는다 — public으로 둬서 MemberAuthController가
    // 참조하게 한다.
    public static final String COOKIE_NAME = "member_refresh_token";
    private static final int MAX_AGE_SECONDS = (int) Duration.ofDays(14).toSeconds();

    private MemberRefreshCookie() {}

    public static void set(HttpServletResponse response, String token) {
        ResponseCookie cookie =
                ResponseCookie.from(COOKIE_NAME, token)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path("/api/auth/member")
                        .maxAge(MAX_AGE_SECONDS)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void clear(HttpServletResponse response) {
        ResponseCookie cookie =
                ResponseCookie.from(COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path("/api/auth/member")
                        .maxAge(0)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
