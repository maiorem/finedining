package com.finediningtheater.artist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaService;
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

@WebMvcTest(ArtistEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArtistEditControllerTest {

    @TestConfiguration
    static class RealSudoModeConfig {
        @Bean
        SudoMode sudoMode() {
            return new SudoMode();
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private SudoMode sudoMode;

    @MockitoBean private ArtistService artistService;
    @MockitoBean private MediaService mediaService;
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
    void 생성에_성공하면_200과_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        when(artistService.create("kim-artist")).thenReturn(new Artist("kim-artist"));

        mockMvc.perform(
                        post("/api/artists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"slug\":\"kim-artist\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("kim-artist"));

        verify(auditLogger).record(eq(1L), eq("ARTIST_CREATE"), eq("Artist"), any(), any(), any(), any());
    }

    @Test
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(artistService.listForAdmin()).thenReturn(List.of(new Artist("kim-artist")));

        mockMvc.perform(get("/api/artists/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("kim-artist"));
    }

    @Test
    void sudo가_열려있지_않으면_발행을_거부한다() throws Exception {
        loginAs(1L);
        when(artistService.getForAdmin(1L)).thenReturn(new Artist("kim-artist"));

        mockMvc.perform(post("/api/artists/1/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }

    @Test
    @DirtiesContext
    void sudo가_열려있으면_발행에_성공한다() throws Exception {
        loginAs(1L);
        sudoMode.activate(1L);
        Artist before = new Artist("kim-artist");
        Artist after = new Artist("kim-artist");
        after.addTranslation(SiteLocale.KO, "김아무개", "연출", "소개");
        after.publish(1L);
        when(artistService.getForAdmin(1L)).thenReturn(before);
        when(artistService.publish(1L, 1L)).thenReturn(after);

        mockMvc.perform(post("/api/artists/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }
}
