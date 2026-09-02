package com.finediningtheater.program;

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
import com.finediningtheater.media.MediaService;
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

@WebMvcTest(ProgramController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProgramService programService;

    // 이미지 파이프라인(§7.5) 연동으로 생긴 의존성. 이 슬라이스는 응답 모양만 보므로 목록은 비워둔다.
    @MockitoBean private MediaService mediaService;

    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 발행된_프로그램_목록을_반환한다() throws Exception {
        Program program = new Program("summer-tasting");
        program.addTranslation(SiteLocale.KO, "여름 시식회", "참가는 구글폼으로");
        program.changeApplyUrl("https://forms.gle/abcd");
        program.changeLocationUrl("https://map.naver.com/p/somewhere");
        when(programService.listPublished()).thenReturn(List.of(program));

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("summer-tasting"))
                .andExpect(jsonPath("$.data[0].title").value("여름 시식회"))
                .andExpect(jsonPath("$.data[0].applyUrl").value("https://forms.gle/abcd"))
                .andExpect(jsonPath("$.data[0].locationUrl").value("https://map.naver.com/p/somewhere"));
    }

    @Test
    void 존재하지_않는_슬러그는_404와_ENTITY_NOT_FOUND를_반환한다() throws Exception {
        when(programService.getPublished("unknown")).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/programs/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
    }

    @Test
    void 비로그인_요청은_preview_파라미터를_무시한다() throws Exception {
        when(programService.getPublished("draft-program")).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/programs/draft-program?preview=true")).andExpect(status().isNotFound());

        verify(programService, never()).getForPreview("draft-program");
    }

    @Test
    void 관리자가_preview를_요청하면_상태_무관으로_조회한다() throws Exception {
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", AdminRole.SUPER_ADMIN);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Program program = new Program("draft-program");
        when(programService.getForPreview("draft-program")).thenReturn(program);

        mockMvc.perform(get("/api/programs/draft-program?preview=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("draft-program"));

        verify(programService, never()).getPublished("draft-program");
    }
}
