package com.finediningtheater.press;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
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

// 실제 hasRole('EDITOR') 인가 검증은 EditControllerSecurityTest + 라이브 스모크 테스트에서 한다.
@WebMvcTest(PressClippingEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class PressClippingEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PressClippingService pressClippingService;
    @MockitoBean private MediaService mediaService;
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
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(pressClippingService.listForAdmin()).thenReturn(List.of(new PressClipping("제목", "https://example.com")));
        when(mediaService.listForAdmin(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/press-clippings/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("제목"));
    }

    @Test
    void 생성에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        PressClipping created = new PressClipping("새 제목", "https://example.com/a");
        when(pressClippingService.create("새 제목", "https://example.com/a")).thenReturn(created);
        when(mediaService.listForAdmin(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(
                        post("/api/press-clippings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"새 제목\",\"externalUrl\":\"https://example.com/a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"));

        verify(auditLogger)
                .record(eq(1L), eq("PRESS_CLIPPING_CREATE"), eq("PressClipping"), any(), any(), any(), any());
    }

    @Test
    void 유효하지_않은_링크는_검증_오류를_반환한다() throws Exception {
        loginAs(1L);

        mockMvc.perform(
                        post("/api/press-clippings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"제목\",\"externalUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 수정에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        PressClipping before = new PressClipping("원래 제목", "https://example.com/old");
        PressClipping after = new PressClipping("새 제목", "https://example.com/new");
        when(pressClippingService.getForAdmin(1L)).thenReturn(before);
        when(pressClippingService.updateContent(1L, "새 제목", "https://example.com/new")).thenReturn(after);
        when(mediaService.listForAdmin(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(
                        put("/api/press-clippings/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"새 제목\",\"externalUrl\":\"https://example.com/new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"));

        verify(auditLogger)
                .record(eq(1L), eq("PRESS_CLIPPING_UPDATE"), eq("PressClipping"), eq(1L), any(), any(), any());
    }

    @Test
    void 발행에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        PressClipping before = new PressClipping("제목", "https://example.com");
        PressClipping after = new PressClipping("제목", "https://example.com");
        after.publish(1L);
        when(pressClippingService.getForAdmin(1L)).thenReturn(before);
        when(pressClippingService.publish(1L, 1L)).thenReturn(after);
        when(mediaService.listForAdmin(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/press-clippings/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(sudoMode).requireActive(1L);
        verify(auditLogger)
                .record(eq(1L), eq("PRESS_CLIPPING_PUBLISH"), eq("PressClipping"), eq(1L), any(), any(), any());
    }

    @Test
    void 발행취소에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        PressClipping before = new PressClipping("제목", "https://example.com");
        before.publish(1L);
        PressClipping after = new PressClipping("제목", "https://example.com");
        when(pressClippingService.getForAdmin(1L)).thenReturn(before);
        when(pressClippingService.unpublish(1L)).thenReturn(after);
        when(mediaService.listForAdmin(any(MediaOwnerType.class), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/press-clippings/1/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(sudoMode).requireActive(1L);
        verify(auditLogger)
                .record(eq(1L), eq("PRESS_CLIPPING_UNPUBLISH"), eq("PressClipping"), eq(1L), any(), any(), any());
    }
}
