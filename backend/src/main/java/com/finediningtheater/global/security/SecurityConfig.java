package com.finediningtheater.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 일반 회원과 관리자가 같은 사이트를 쓰지만 인증 수단은 다르다(§1). 여기서 다루는 건 관리자
 * 세션뿐이다 — 카카오 OAuth2는 우선순위 마지막이라 아직 없다.
 *
 * <p>Bearer 토큰 기반 stateless API라 CSRF 필터를 끈다. 실제 쓰기는 전부 Authorization 헤더로
 * 인증되므로(브라우저가 자동으로 실어 나르지 않는다) CSRF 공격면이 없다. refresh 토큰만 쿠키에
 * 있는데, HttpOnly + SameSite=Lax만으로 막는다 — Lax는 교차 사이트 POST/fetch에 쿠키를 붙이지
 * 않으므로 이 요청 하나 때문에 전체 API에 이중 제출 토큰 핸드셰이크를 강제하지 않는다
 * (CLAUDE.md §3.5 "CSRF 방어"의 취지를 stateless API에 맞게 적용한 것 — 2026-08-27 결정).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins}") String corsAllowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(",")).map(String::trim).toList();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SUPER_ADMIN은 EDITOR가 할 수 있는 건 전부 할 수 있어야 한다. 계층 없이는 JWT에
     * ROLE_SUPER_ADMIN 하나만 실려서 {@code hasRole('EDITOR')} 검사를 통과하지 못한다 — 이 두
     * static 빈이 있어야 method security가 계층을 실제로 적용한다(Spring Security의 요구사항).
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_EDITOR");
    }

    @Bean
    static DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                // /api/auth/admin/pin·sudo는 여기 넣지 않는다 — 인증된 관리자만 써야 하므로
                                // 아래 "/api/**" → authenticated() 규칙으로 자연히 떨어진다.
                                auth.requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/admin/login",
                                                "/api/auth/admin/refresh",
                                                "/api/auth/admin/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/productions/**",
                                                "/api/showings/**",
                                                "/api/artists/**",
                                                "/api/castings/**")
                                        // /manage 하위 경로도 이 와일드카드에 걸리지만 안전하다 — 그쪽은
                                        // ArtistEditController/CastingEditController의 클래스 레벨
                                        // @PreAuthorize가 별도 AOP 계층에서 여전히 막는다. 여기서
                                        // permitAll은 "필터 체인을 통과시킨다"는 뜻이지 인가를 면제하지 않는다.
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/showings/*/booking-click")
                                        .permitAll()
                                        // 협업제안은 카카오 로그인이 붙기 전까지 로그인 없이 받는다(2026-08-27, §3.7).
                                        .requestMatchers(HttpMethod.POST, "/api/proposals")
                                        .permitAll()
                                        .requestMatchers("/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers("/api/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(this::handleUnauthenticated)
                                        .accessDeniedHandler(this::handleAccessDenied));
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void handleUnauthenticated(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        writeError(response, ErrorCode.UNAUTHORIZED);
    }

    private void handleAccessDenied(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        writeError(response, ErrorCode.FORBIDDEN);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
