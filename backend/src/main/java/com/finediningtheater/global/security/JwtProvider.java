package com.finediningtheater.global.security;

import com.finediningtheater.account.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 관리자·일반 회원 세션 공용 JWT 발급·검증. 로그인 수단(관리자는 아이디·비밀번호, 회원은
 * 카카오 OAuth2)은 서로 다르지만 발급하는 토큰의 모양과 이후 처리는 하나로 수렴한다(CLAUDE.md
 * §7.4). {@code principalType} 클레임으로 관리자용 토큰과 회원용 토큰을 서로 다른 자격으로
 * 쓸 수 없게 분리한다 — 관리자 refresh 토큰으로 회원 세션을 재발급받을 수 없다.
 *
 * <p>OAuth2 리소스서버 전체를 끌어오지 않고 최소한만 직접 구현한다 — 운영자·회원 규모가 작아
 * JWK 회전 같은 인프라가 필요 없다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_PRINCIPAL_TYPE = "principalType";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_NICKNAME = "nickname";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String PRINCIPAL_ADMIN = "ADMIN";
    private static final String PRINCIPAL_MEMBER = "MEMBER";

    private final SecretKey key;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-minutes}") long accessTokenValidityMinutes,
            @Value("${app.jwt.refresh-token-validity-days}") long refreshTokenValidityDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofMinutes(accessTokenValidityMinutes);
        this.refreshTokenValidity = Duration.ofDays(refreshTokenValidityDays);
    }

    // ── 관리자 ──────────────────────────────────────────────────────────

    public String createAdminAccessToken(Long adminId, String username, AdminRole role) {
        return buildToken(
                adminId,
                accessTokenValidity,
                Map.of(
                        CLAIM_TYPE, TYPE_ACCESS,
                        CLAIM_PRINCIPAL_TYPE, PRINCIPAL_ADMIN,
                        CLAIM_USERNAME, username,
                        CLAIM_ROLE, role.name()));
    }

    public String createAdminRefreshToken(Long adminId) {
        return buildToken(
                adminId, refreshTokenValidity, Map.of(CLAIM_TYPE, TYPE_REFRESH, CLAIM_PRINCIPAL_TYPE, PRINCIPAL_ADMIN));
    }

    /** 서명·만료·타입·principalType이 전부 유효한 관리자 access 토큰만 통과시킨다. */
    public AdminAccessTokenClaims parseAdminAccessToken(String token) {
        Claims claims = parseTypedClaims(token, TYPE_ACCESS, PRINCIPAL_ADMIN);
        return new AdminAccessTokenClaims(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                AdminRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public Long parseAdminRefreshToken(String token) {
        Claims claims = parseTypedClaims(token, TYPE_REFRESH, PRINCIPAL_ADMIN);
        return Long.valueOf(claims.getSubject());
    }

    // ── 일반 회원(카카오) ────────────────────────────────────────────────

    public String createMemberAccessToken(Long accountId, String nickname) {
        return buildToken(
                accountId,
                accessTokenValidity,
                Map.of(
                        CLAIM_TYPE, TYPE_ACCESS,
                        CLAIM_PRINCIPAL_TYPE, PRINCIPAL_MEMBER,
                        CLAIM_NICKNAME, nickname));
    }

    public String createMemberRefreshToken(Long accountId) {
        return buildToken(
                accountId,
                refreshTokenValidity,
                Map.of(CLAIM_TYPE, TYPE_REFRESH, CLAIM_PRINCIPAL_TYPE, PRINCIPAL_MEMBER));
    }

    public MemberAccessTokenClaims parseMemberAccessToken(String token) {
        Claims claims = parseTypedClaims(token, TYPE_ACCESS, PRINCIPAL_MEMBER);
        return new MemberAccessTokenClaims(Long.valueOf(claims.getSubject()), claims.get(CLAIM_NICKNAME, String.class));
    }

    public Long parseMemberRefreshToken(String token) {
        Claims claims = parseTypedClaims(token, TYPE_REFRESH, PRINCIPAL_MEMBER);
        return Long.valueOf(claims.getSubject());
    }

    // ── 공통 ────────────────────────────────────────────────────────────

    private String buildToken(Long subjectId, Duration validity, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    private Claims parseTypedClaims(String token, String expectedType, String expectedPrincipalType) {
        Claims claims = parseClaims(token);
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("토큰 타입이 올바르지 않습니다.");
        }
        if (!expectedPrincipalType.equals(claims.get(CLAIM_PRINCIPAL_TYPE, String.class))) {
            throw new JwtException("이 토큰으로는 이 세션에 접근할 수 없습니다.");
        }
        return claims;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public record AdminAccessTokenClaims(Long adminId, String username, AdminRole role) {}

    public record MemberAccessTokenClaims(Long accountId, String nickname) {}
}
