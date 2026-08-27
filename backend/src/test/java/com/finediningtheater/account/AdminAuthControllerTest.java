package com.finediningtheater.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 실제 보안 필터 체인 검증은 JwtAuthenticationFilterTest에서 한다. 여기서는 컨트롤러의
// 요청/응답 매핑, 쿠키 발급, 레이트리밋 위임만 확인한다.
@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminAuthService adminAuthService;
    @MockitoBean private RateLimiter rateLimiter;

    // JwtAuthenticationFilter가 Filter로 스캔되면서 딸려오는 의존성 — 이 슬라이스에선 실행되지
    // 않지만(addFilters=false) 빈 그래프를 만족시켜야 컨텍스트가 뜬다.
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 로그인_성공하면_액세스_토큰과_refresh_쿠키를_돌려준다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(adminAuthService.login("admin", "ChangeMe!2026"))
                .thenReturn(new AdminSession("access-token", "refresh-token", "admin", AdminRole.SUPER_ADMIN));

        mockMvc.perform(
                        post("/api/auth/admin/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"ChangeMe!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
                .andExpect(cookie().exists("admin_refresh_token"))
                .andExpect(cookie().httpOnly("admin_refresh_token", true));
    }

    @Test
    void 레이트리밋을_넘으면_429를_반환하고_로그인을_시도조차_하지_않는다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(
                        post("/api/auth/admin/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));

        verifyNoInteractions(adminAuthService);
    }

    @Test
    void 자격증명이_틀리면_401을_반환한다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(adminAuthService.login(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(
                        post("/api/auth/admin/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 빈_아이디로_로그인하면_검증_오류를_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/auth/admin/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void refresh_쿠키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/admin/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_refresh_쿠키면_새_토큰을_돌려준다() throws Exception {
        when(adminAuthService.refresh("valid-refresh-token"))
                .thenReturn(new AdminSession("new-access-token", "new-refresh-token", "admin", AdminRole.EDITOR));

        mockMvc.perform(
                        post("/api/auth/admin/refresh")
                                .cookie(new Cookie("admin_refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(cookie().value("admin_refresh_token", "new-refresh-token"));
    }

    @Test
    void 로그아웃하면_refresh_쿠키를_만료시킨다() throws Exception {
        mockMvc.perform(post("/api/auth/admin/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("admin_refresh_token", 0));
    }

    // addFilters=false라 필터 체인이 @AuthenticationPrincipal용 SecurityContext를 채워주지
    // 않는다. MockMvc가 테스트 스레드에서 동기 실행되는 걸 이용해 직접 채운다.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id, AdminRole role) {
        AdminPrincipal principal = new AdminPrincipal(id, "admin", role);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void PIN_설정에_성공하면_200을_반환한다() throws Exception {
        loginAs(1L, AdminRole.SUPER_ADMIN);

        mockMvc.perform(
                        post("/api/auth/admin/pin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\":\"pw\",\"newPin\":\"482913\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void PIN_형식이_틀리면_검증_오류를_반환한다() throws Exception {
        loginAs(1L, AdminRole.SUPER_ADMIN);

        mockMvc.perform(
                        post("/api/auth/admin/pin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\":\"pw\",\"newPin\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void sudo_확인에_성공하면_200을_반환한다() throws Exception {
        loginAs(1L, AdminRole.SUPER_ADMIN);

        mockMvc.perform(
                        post("/api/auth/admin/sudo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\":\"482913\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void PIN이_틀리면_sudo_확인은_401을_반환한다() throws Exception {
        loginAs(1L, AdminRole.SUPER_ADMIN);
        doThrow(new BusinessException(ErrorCode.PIN_INVALID))
                .when(adminAuthService)
                .verifySudo(eq(1L), anyString());

        mockMvc.perform(
                        post("/api/auth/admin/sudo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pin\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("PIN_INVALID"));
    }
}
