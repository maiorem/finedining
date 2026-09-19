package com.finediningtheater.site;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.SudoMode;
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

// 실제 hasRole('SUPER_ADMIN') 인가 검증은 EditControllerSecurityTest + 라이브 스모크 테스트에서 한다.
@WebMvcTest(SiteEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class SiteEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SiteVisibilityService siteVisibilityService;
    @MockitoBean private AuditLogger auditLogger;
    @MockitoBean private SudoMode sudoMode;
    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id) {
        AdminPrincipal principal = new AdminPrincipal(id, "admin", AdminRole.SUPER_ADMIN);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 공개로_전환하면_저장하고_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        when(siteVisibilityService.isPublic()).thenReturn(false);

        mockMvc.perform(put("/api/site/visibility").contentType(MediaType.APPLICATION_JSON).content("{\"open\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(true));

        verify(sudoMode).requireActive(1L);
        verify(siteVisibilityService).setPublic(true);
        verify(auditLogger)
                .record(eq(1L), eq("SITE_VISIBILITY_CHANGE"), eq("SiteSetting"), any(), any(), any(), any());
    }

    @Test
    void PIN_확인이_안_된_상태면_전환하지_않는다() throws Exception {
        loginAs(1L);
        doThrow(new BusinessException(ErrorCode.PIN_REQUIRED)).when(sudoMode).requireActive(1L);

        mockMvc.perform(put("/api/site/visibility").contentType(MediaType.APPLICATION_JSON).content("{\"open\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));

        verify(siteVisibilityService, never()).setPublic(true);
    }

    @Test
    void open_필드가_없으면_검증_오류를_반환하고_아무것도_바꾸지_않는다() throws Exception {
        loginAs(1L);

        mockMvc.perform(put("/api/site/visibility").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(siteVisibilityService, never()).setPublic(false);
    }
}
