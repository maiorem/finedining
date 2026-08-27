package com.finediningtheater.production;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 실제 인가 필터(hasRole('EDITOR')·PreAuthorize) 검증은 EditControllerSecurityTest +
// 라이브 스모크 테스트에서 한다. 여기서는 컨트롤러의 요청/응답 매핑, sudo 게이트, 감사 로그
// 호출만 확인한다. SudoMode는 진짜 인스턴스를 빈으로 등록해 activate() 여부가 실제로
// publish/unpublish를 막는지 검증한다.
@WebMvcTest(ProductionEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductionEditControllerTest {

    @TestConfiguration
    static class RealSudoModeConfig {
        @Bean
        SudoMode sudoMode() {
            return new SudoMode();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private SudoMode sudoMode;

    @MockitoBean private ProductionService productionService;
    @MockitoBean private AuditLogger auditLogger;
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
    void 작품_생성에_성공하면_200과_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        when(productionService.create("showcase")).thenReturn(new Production("showcase"));

        mockMvc.perform(
                        post("/api/productions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"slug\":\"showcase\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("showcase"));

        verify(auditLogger)
                .record(eq(1L), eq("PRODUCTION_CREATE"), eq("Production"), any(), any(), any(), any());
    }

    @Test
    void 슬러그_형식이_틀리면_검증_오류를_반환한다() throws Exception {
        loginAs(1L);

        mockMvc.perform(
                        post("/api/productions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"slug\":\"Not Valid Slug!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 중복_슬러그는_409를_반환한다() throws Exception {
        loginAs(1L);
        when(productionService.create("showcase")).thenThrow(new BusinessException(ErrorCode.DUPLICATE_SLUG));

        mockMvc.perform(
                        post("/api/productions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"slug\":\"showcase\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SLUG"));
    }

    @Test
    void 임시저장에_성공하면_200을_반환하고_공개본은_건드리지_않는다() throws Exception {
        loginAs(1L);
        when(productionService.getForAdmin(1L)).thenReturn(new Production("showcase"));

        mockMvc.perform(
                        put("/api/productions/1/translations/KO")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"새 제목\",\"subtitle\":null}"))
                .andExpect(status().isOk());

        verify(productionService).saveDraftTranslation(1L, SiteLocale.KO, "새 제목", null);
    }

    @Test
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(productionService.listForAdmin()).thenReturn(List.of(new Production("showcase")));

        mockMvc.perform(get("/api/productions/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("showcase"));
    }

    @Test
    void sudo가_열려있지_않으면_발행을_거부한다() throws Exception {
        loginAs(1L);
        when(productionService.getForAdmin(1L)).thenReturn(new Production("showcase"));

        mockMvc.perform(post("/api/productions/1/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }

    // sudoMode 빈은 이 테스트 클래스 안에서 컨텍스트가 캐시되는 한 공유된다 — activate() 이후
    // 다른 테스트에 상태가 새지 않게 이 테스트 다음엔 컨텍스트를 버린다.
    @Test
    @DirtiesContext
    void sudo가_열려있으면_발행에_성공하고_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        sudoMode.activate(1L);
        Production before = new Production("showcase");
        Production after = new Production("showcase");
        after.addTranslation(SiteLocale.KO, "쇼케이스", null);
        after.publish(1L);
        when(productionService.getForAdmin(1L)).thenReturn(before);
        when(productionService.publish(1L, 1L)).thenReturn(after);

        mockMvc.perform(post("/api/productions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(auditLogger)
                .record(eq(1L), eq("PRODUCTION_PUBLISH"), eq("Production"), eq(1L), any(), any(), any());
    }

    @Test
    void sudo가_열려있지_않으면_발행취소도_거부한다() throws Exception {
        loginAs(1L);
        when(productionService.getForAdmin(1L)).thenReturn(new Production("showcase"));

        mockMvc.perform(post("/api/productions/1/unpublish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }
}
