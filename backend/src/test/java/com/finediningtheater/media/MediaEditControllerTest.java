package com.finediningtheater.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.account.AdminRole;
import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
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
// 여기서는 요청/응답 매핑과 감사 로그 호출만 확인한다.
@WebMvcTest(MediaEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaEditControllerTest {

    @Autowired private MockMvc mockMvc;

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
    void presign_요청이_유효하면_업로드_URL을_반환한다() throws Exception {
        loginAs(1L);
        when(mediaService.presign(MediaOwnerType.PRODUCTION, 1L, 1L, "image/jpeg", 1000))
                .thenReturn(new MediaService.PresignResult(10L, "http://localhost:9000/x"));

        mockMvc.perform(
                        post("/api/media/presign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"ownerType\":\"PRODUCTION\",\"ownerId\":1,\"contentType\":\"image/jpeg\",\"contentLengthBytes\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaAssetId").value(10))
                .andExpect(jsonPath("$.data.uploadUrl").value("http://localhost:9000/x"));
    }

    @Test
    void 완료_콜백은_감사로그를_남기고_응답을_반환한다() throws Exception {
        loginAs(1L);
        MediaAsset asset = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        when(mediaService.completeUpload(eq(10L), eq("설명"))).thenReturn(asset);

        mockMvc.perform(
                        post("/api/media/10/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"altText\":\"설명\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(auditLogger)
                .record(eq(1L), eq("MEDIA_UPLOAD_COMPLETE"), eq("MediaAsset"), eq(10L), any(), any(), any());
    }

    @Test
    void 캡션_수정은_이전_캡션을_감사로그에_남기고_새_캡션을_반환한다() throws Exception {
        loginAs(1L);
        MediaAsset before = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        before.markReady(100, 100, "d640", "d960", "d1600", "lqip", "옛 캡션");
        MediaAsset after = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        after.markReady(100, 100, "d640", "d960", "d1600", "lqip", "새 캡션");
        when(mediaService.get(10L)).thenReturn(before);
        when(mediaService.updateAltText(10L, "새 캡션")).thenReturn(after);

        mockMvc.perform(put("/api/media/10").contentType(MediaType.APPLICATION_JSON).content("{\"altText\":\"새 캡션\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.altText").value("새 캡션"));

        verify(auditLogger)
                .record(eq(1L), eq("MEDIA_ALT_TEXT_UPDATE"), eq("MediaAsset"), eq(10L), eq("옛 캡션"), eq("새 캡션"), any());
    }

    @Test
    void 설명_문단_수정은_이전_값을_감사로그에_남기고_새_값을_반환한다() throws Exception {
        loginAs(1L);
        MediaAsset before = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        MediaAsset after = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        after.updateCaption("새 설명");
        when(mediaService.get(10L)).thenReturn(before);
        when(mediaService.updateCaption(10L, "새 설명")).thenReturn(after);

        mockMvc.perform(
                        put("/api/media/10/caption")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"caption\":\"새 설명\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caption").value("새 설명"));

        verify(auditLogger)
                .record(eq(1L), eq("MEDIA_CAPTION_UPDATE"), eq("MediaAsset"), eq(10L), any(), eq("새 설명"), any());
    }

    @Test
    void 삭제는_감사로그를_남긴다() throws Exception {
        loginAs(1L);

        mockMvc.perform(delete("/api/media/10")).andExpect(status().isOk());

        verify(mediaService).delete(10L);
        verify(auditLogger).record(eq(1L), eq("MEDIA_DELETE"), eq("MediaAsset"), eq(10L), any(), any(), any());
    }
}
