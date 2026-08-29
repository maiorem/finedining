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
        String token = jwtProvider.createAdminAccessToken(1L, "admin", AdminRole.SUPER_ADMIN);

        JwtProvider.AdminAccessTokenClaims claims = jwtProvider.parseAdminAccessToken(token);

        assertThat(claims.adminId()).isEqualTo(1L);
        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.role()).isEqualTo(AdminRole.SUPER_ADMIN);
    }

    @Test
    void refresh_토큰을_만들고_그대로_파싱할_수_있다() {
        String token = jwtProvider.createAdminRefreshToken(7L);

        Long adminId = jwtProvider.parseAdminRefreshToken(token);

        assertThat(adminId).isEqualTo(7L);
    }

    @Test
    void refresh_토큰을_access_토큰으로_파싱하면_거부한다() {
        String refreshToken = jwtProvider.createAdminRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseAdminAccessToken(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void access_토큰을_refresh_토큰으로_파싱하면_거부한다() {
        String accessToken = jwtProvider.createAdminAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> jwtProvider.parseAdminRefreshToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 변조된_토큰은_거부한다() {
        String token = jwtProvider.createAdminAccessToken(1L, "admin", AdminRole.EDITOR);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtProvider.parseAdminAccessToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_거부한다() {
        JwtProvider otherProvider =
                new JwtProvider("another-test-secret-key-also-at-least-32-bytes-long", 15, 14);
        String token = otherProvider.createAdminAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> jwtProvider.parseAdminAccessToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void 회원_access_토큰을_만들고_그대로_파싱할_수_있다() {
        String token = jwtProvider.createMemberAccessToken(42L, "김아무개");

        JwtProvider.MemberAccessTokenClaims claims = jwtProvider.parseMemberAccessToken(token);

        assertThat(claims.accountId()).isEqualTo(42L);
        assertThat(claims.nickname()).isEqualTo("김아무개");
    }

    @Test
    void 회원_refresh_토큰을_만들고_그대로_파싱할_수_있다() {
        String token = jwtProvider.createMemberRefreshToken(42L);

        assertThat(jwtProvider.parseMemberRefreshToken(token)).isEqualTo(42L);
    }

    @Test
    void 관리자_access_토큰을_회원_access_토큰으로_파싱하면_거부한다() {
        String adminToken = jwtProvider.createAdminAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> jwtProvider.parseMemberAccessToken(adminToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void 회원_access_토큰을_관리자_access_토큰으로_파싱하면_거부한다() {
        String memberToken = jwtProvider.createMemberAccessToken(42L, "김아무개");

        assertThatThrownBy(() -> jwtProvider.parseAdminAccessToken(memberToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void 관리자_refresh_토큰을_회원_refresh_엔드포인트에_쓰면_거부한다() {
        String adminRefresh = jwtProvider.createAdminRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseMemberRefreshToken(adminRefresh)).isInstanceOf(JwtException.class);
    }
}
