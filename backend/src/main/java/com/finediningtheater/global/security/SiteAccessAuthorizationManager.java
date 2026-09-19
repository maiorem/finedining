package com.finediningtheater.global.security;

import com.finediningtheater.site.SiteVisibilityService;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * 사이트가 비공개인 동안(오픈 전, 2026-09-19) /api/**를 관리자에게만 연다. 공개 상태면 기존 규칙을
 * 그대로 따른다 — requireAuthentication은 기존 "/api/** → authenticated()" 규칙과 공개 GET의
 * permitAll을 같은 매니저로 표현하려고 둔 스위치다. 프론트의 "준비 중" 화면은 편의일 뿐이고
 * 실제로 막는 건 여기다(§3.5 "서버가 막지 않으면 막힌 게 아니다").
 */
public class SiteAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final SiteVisibilityService siteVisibilityService;
    private final boolean requireAuthentication;

    public SiteAccessAuthorizationManager(SiteVisibilityService siteVisibilityService, boolean requireAuthentication) {
        this.siteVisibilityService = siteVisibilityService;
        this.requireAuthentication = requireAuthentication;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (!siteVisibilityService.isPublic()) {
            return new AuthorizationDecision(isAuthenticated(current) && current.getPrincipal() instanceof AdminPrincipal);
        }
        return new AuthorizationDecision(!requireAuthentication || isAuthenticated(current));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
