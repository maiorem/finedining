package com.finediningtheater.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.account.AdminRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider =
            new JwtProvider("test-only-secret-key-must-be-at-least-32-bytes-long-for-hs256", 15, 14);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void filter(HttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};
        filter.doFilter(request, response, chain);
    }

    @Test
    void 유효한_토큰이면_SecurityContext에_관리자_주체를_채운다() throws Exception {
        String token = jwtProvider.createAccessToken(1L, "admin", AdminRole.SUPER_ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter(request);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.username()).isEqualTo("admin");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void 토큰이_없으면_SecurityContext를_건드리지_않는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void Bearer_접두사가_없으면_무시한다() throws Exception {
        String token = jwtProvider.createAccessToken(1L, "admin", AdminRole.EDITOR);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        filter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 위조된_토큰이면_SecurityContext를_비운다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-jwt");

        filter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void refresh_토큰을_Authorization_헤더에_넣으면_거부한다() throws Exception {
        String refreshToken = jwtProvider.createRefreshToken(1L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);

        filter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
