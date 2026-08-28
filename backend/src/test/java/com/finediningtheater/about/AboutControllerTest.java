package com.finediningtheater.about;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AboutController.class)
@AutoConfigureMockMvc(addFilters = false)
class AboutControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AboutService aboutService;
    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 발행된_소개문을_반환한다() throws Exception {
        AboutContent about = new AboutContent();
        about.addTranslation(SiteLocale.KO, "파인다이닝 씨어터 소개");
        when(aboutService.getPublished()).thenReturn(about);

        mockMvc.perform(get("/api/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intro").value("파인다이닝 씨어터 소개"));
    }

    @Test
    void 아직_발행된_적_없으면_404를_반환한다() throws Exception {
        when(aboutService.getPublished()).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/about")).andExpect(status().isNotFound());
    }

    @Test
    void 비로그인_요청은_preview_파라미터를_무시한다() throws Exception {
        when(aboutService.getPublished()).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/about?preview=true")).andExpect(status().isNotFound());

        verify(aboutService, never()).getForPreview();
    }

    @Test
    void 관리자가_preview를_요청하면_상태_무관으로_조회한다() throws Exception {
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", AdminRole.SUPER_ADMIN);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        AboutContent about = new AboutContent();
        about.addTranslation(SiteLocale.KO, "초안 소개문");
        when(aboutService.getForPreview()).thenReturn(about);

        mockMvc.perform(get("/api/about?preview=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intro").value("초안 소개문"));

        verify(aboutService, never()).getPublished();
    }
}
