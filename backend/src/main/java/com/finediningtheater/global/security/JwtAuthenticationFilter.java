package com.finediningtheater.global.security;

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
 * Authorization: Bearer 헤더의 access 토큰을 읽어 SecurityContext를 채운다. 토큰이 없거나
 * 유효하지 않으면 그냥 다음 필터로 넘긴다 — 401/403 판정은 뒤따르는 authorizeHttpRequests와
 * 예외 핸들러(SecurityConfig)가 한다 (CLAUDE.md §3.5).
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
            try {
                JwtProvider.AccessTokenClaims claims =
                        jwtProvider.parseAccessToken(header.substring(BEARER_PREFIX.length()));
                AdminPrincipal principal =
                        new AdminPrincipal(claims.adminId(), claims.username(), claims.role());
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
                var authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
