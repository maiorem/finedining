package com.finediningtheater.global.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 헤더의 access 토큰을 읽어 SecurityContext를 채운다. 관리자 토큰과
 * 회원 토큰은 같은 헤더 자리를 쓰지만 서로 다른 principalType 클레임을 갖는다(§7.4) — 먼저
 * 관리자 토큰으로 파싱을 시도하고, 실패하면 회원 토큰으로 다시 시도한다. 둘 다 실패하면 그냥
 * 다음 필터로 넘긴다 — 401/403 판정은 뒤따르는 authorizeHttpRequests와 예외 핸들러
 * (SecurityConfig)가 한다 (CLAUDE.md §3.5).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            if (!authenticateAsAdmin(token) && !authenticateAsMember(token)) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateAsAdmin(String token) {
        try {
            JwtProvider.AdminAccessTokenClaims claims = jwtProvider.parseAdminAccessToken(token);
            AdminPrincipal principal = new AdminPrincipal(claims.adminId(), claims.username(), claims.role());
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean authenticateAsMember(String token) {
        try {
            JwtProvider.MemberAccessTokenClaims claims = jwtProvider.parseMemberAccessToken(token);
            MemberPrincipal principal = new MemberPrincipal(claims.accountId(), claims.nickname());
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
