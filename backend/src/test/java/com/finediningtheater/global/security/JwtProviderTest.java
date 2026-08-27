package com.finediningtheater.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finediningtheater.account.AdminRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private final JwtProvider jwtProvider =
            new JwtProvider(
                    "test-only-secret-key-must-be-at-least-32-bytes-long-for-hs256", 15, 14);

    @Test
    void access_토큰을_만들고_그대로_파싱할_수_있다() {
        String token = jwtProvider.createAccessToken(1L, "admin", AdminRole.SUPER_ADMIN);

        JwtProvider.AccessTokenClaims claims = jwtProvider.parseAccessToken(token);

        assertThat(claims.adminId()).isEqualTo(1L);
        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.role()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    @Test
    void refresh_토큰을_만들고_그대로_파싱할_수_있다() {
        String token = jwtProvider.createRefreshToken(7L);

        Long adminId = jwtProvider.parseRefreshToken(token);

        assertThat(adminId).isEqualTo(7L);
    }

    @Test
    void refresh_토큰을_access_토큰으로_파싱하면_거부한다() {
        String refreshToken = jwtProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void access_토큰을_refresh_토큰으로_파싱하면_거부한다() {
        String accessToken = jwtProvider.createAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> jwtProvider.parseRefreshToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 변조된_토큰은_거부한다() {
        String token = jwtProvider.createAccessToken(1L, "admin", AdminRole.EDITOR);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_거부한다() {
        JwtProvider otherProvider =
                new JwtProvider("another-test-secret-key-also-at-least-32-bytes-long", 15, 14);
        String token = otherProvider.createAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(token)).isInstanceOf(JwtException.class);
    }
}
