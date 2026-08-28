package com.finediningtheater.artist;

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

@WebMvcTest(ArtistController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArtistControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ArtistService artistService;

    // 이미지 파이프라인(§7.5) 연동으로 생긴 의존성. 이 슬라이스는 응답 모양만 보므로 목록은 비워둔다.
    @MockitoBean private MediaService mediaService;

    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 발행된_아티스트_목록을_반환한다() throws Exception {
        Artist artist = new Artist("kim-artist");
        artist.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개", "참여작품");
        when(artistService.listPublished()).thenReturn(List.of(artist));

        mockMvc.perform(get("/api/artists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("kim-artist"))
                .andExpect(jsonPath("$.data[0].name").value("김아무개"));
    }

    @Test
    void 존재하지_않는_슬러그는_404를_반환한다() throws Exception {
        when(artistService.getPublished("unknown")).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/artists/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void 비로그인_요청은_preview_파라미터를_무시한다() throws Exception {
        when(artistService.getPublished("draft-artist")).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/artists/draft-artist?preview=true")).andExpect(status().isNotFound());

        verify(artistService, never()).getForPreview("draft-artist");
    }

    @Test
    void 관리자가_preview를_요청하면_상태_무관으로_조회한다() throws Exception {
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", AdminRole.SUPER_ADMIN);
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Artist artist = new Artist("draft-artist");
        when(artistService.getForPreview("draft-artist")).thenReturn(artist);

        mockMvc.perform(get("/api/artists/draft-artist?preview=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("draft-artist"));

        verify(artistService, never()).getPublished("draft-artist");
    }
}
