package com.finediningtheater.global.security;

import com.finediningtheater.account.Account;
import com.finediningtheater.account.AccountService;
import com.finediningtheater.account.MemberAuthService;
import com.finediningtheater.account.MemberRefreshCookie;
import com.finediningtheater.account.MemberSession;
import com.finediningtheater.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 카카오 로그인 성공 후 계정을 찾거나 만들고, 우리 자체 JWT로 세션을 발급해 프론트로 리다이렉트
 * 한다(CLAUDE.md §7.4). Spring이 관리하는 OAuth2 세션은 여기서 끝이다 — 이후 요청은 전부 이
 * access token(Authorization 헤더)으로 인증한다.
 *
 * <p>access token은 URL 프래그먼트(#)로 넘긴다 — 쿼리스트링과 달리 서버 로그·Referer에 남지
 * 않는다. refresh token은 관리자와 동일한 모양으로 HttpOnly 쿠키에 담는다.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountService accountService;
    private final MemberAuthService memberAuthService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            AccountService accountService,
            MemberAuthService memberAuthService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.accountService = accountService;
        this.memberAuthService = memberAuthService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = "kakao";

        String providerUserId = String.valueOf(oAuth2User.getAttributes().get("id"));
        Map<String, Object> kakaoAccount = attributeMap(oAuth2User.getAttributes().get("kakao_account"));
        Map<String, Object> profile = attributeMap(kakaoAccount.get("profile"));
        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.getOrDefault("nickname", "회원");

        try {
            Account account = accountService.findOrCreate(provider, providerUserId, email, nickname);
            MemberSession session = memberAuthService.issueSession(account);
            MemberRefreshCookie.set(response, session.refreshToken());
            response.sendRedirect(
                    frontendUrl
                            + "/oauth/callback#accessToken="
                            + encode(session.accessToken())
                            + "&nickname="
                            + encode(session.nickname())
                            + "&accountId="
                            + session.accountId());
        } catch (BusinessException e) {
            response.sendRedirect(frontendUrl + "/login?error=" + encode(e.getErrorCode().getCode()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> attributeMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
