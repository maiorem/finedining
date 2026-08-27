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
 * 관리자 세션 전용 JWT 발급·검증. 일반 회원(카카오)의 OAuth2 흐름과는 별개다 (CLAUDE.md §7.4).
 * OAuth2 리소스서버 전체를 끌어오지 않고 최소한만 직접 구현한다 — 운영자가 소수라 JWK 회전 같은
 * 인프라가 필요 없다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USERNAME = "username";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

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

    public String createAccessToken(Long adminId, String username, AdminRole role) {
        return buildToken(
                adminId,
                accessTokenValidity,
                Map.of(CLAIM_TYPE, TYPE_ACCESS, CLAIM_USERNAME, username, CLAIM_ROLE, role.name()));
    }

    public String createRefreshToken(Long adminId) {
        return buildToken(adminId, refreshTokenValidity, Map.of(CLAIM_TYPE, TYPE_REFRESH));
    }

    private String buildToken(Long adminId, Duration validity, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** 서명·만료·타입이 전부 유효한 access 토큰만 통과시킨다. 실패하면 예외를 던진다. */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("access 토큰이 아닙니다.");
        }
        return new AccessTokenClaims(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                AdminRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    /** refresh 토큰에서 관리자 id만 뽑아낸다. 실패하면 예외를 던진다. */
    public Long parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("refresh 토큰이 아닙니다.");
        }
        return Long.valueOf(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public record AccessTokenClaims(Long adminId, String username, AdminRole role) {}
}
