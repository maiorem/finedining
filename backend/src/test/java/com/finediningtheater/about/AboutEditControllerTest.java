package com.finediningtheater.about;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.finediningtheater.global.support.SiteLocale;
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

@WebMvcTest(AboutEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AboutEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AboutService aboutService;
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
    void 관리자_조회에_성공한다() throws Exception {
        loginAs(1L);
        when(aboutService.getForAdmin()).thenReturn(new AboutContent());

        mockMvc.perform(get("/api/about/manage")).andExpect(status().isOk());
    }

    @Test
    void 임시저장에_성공한다() throws Exception {
        loginAs(1L);
        when(aboutService.getForAdmin()).thenReturn(new AboutContent());

        mockMvc.perform(
                        put("/api/about/translations/KO")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"intro\":\"새 소개문\"}"))
                .andExpect(status().isOk());

        verify(aboutService).saveDraftTranslation(SiteLocale.KO, "새 소개문");
    }

    @Test
    void sudo가_열려있지_않으면_발행을_거부한다() throws Exception {
        loginAs(1L);
        doThrow(new BusinessException(ErrorCode.PIN_REQUIRED)).when(sudoMode).requireActive(1L);

        mockMvc.perform(post("/api/about/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }

    @Test
    void sudo가_열려있으면_발행에_성공하고_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        AboutContent before = new AboutContent();
        AboutContent after = new AboutContent();
        after.addTranslation(SiteLocale.KO, "새 소개문");
        after.publish(1L);
        when(aboutService.getForAdmin()).thenReturn(before);
        when(aboutService.publish(1L)).thenReturn(after);

        mockMvc.perform(post("/api/about/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(auditLogger).record(eq(1L), eq("ABOUT_PUBLISH"), eq("AboutContent"), any(), any(), any(), any());
    }
}
