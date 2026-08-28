package com.finediningtheater.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@WebMvcTest(ReviewEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReviewService reviewService;
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
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(reviewService.listForAdmin()).thenReturn(List.of(new Review(1L, "제목", "본문")));

        mockMvc.perform(get("/api/reviews/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("제목"));
    }

    @Test
    void 원문_수정에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        Review before = new Review(1L, "원래 제목", "원래 본문");
        Review after = new Review(1L, "새 제목", "새 본문");
        when(reviewService.getForAdmin(1L)).thenReturn(before);
        when(reviewService.adminEditContent(1L, "새 제목", "새 본문")).thenReturn(after);

        mockMvc.perform(
                        put("/api/reviews/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"새 제목\",\"body\":\"새 본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"));

        verify(auditLogger)
                .record(eq(1L), eq("REVIEW_ADMIN_EDIT"), eq("Review"), eq(1L), any(), any(), any());
    }

    @Test
    void 숨김에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        Review before = new Review(1L, "제목", "본문");
        Review after = new Review(1L, "제목", "본문");
        after.hide();
        when(reviewService.getForAdmin(1L)).thenReturn(before);
        when(reviewService.hide(1L)).thenReturn(after);

        mockMvc.perform(post("/api/reviews/1/hide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        verify(auditLogger).record(eq(1L), eq("REVIEW_HIDE"), eq("Review"), eq(1L), any(), any(), any());
    }

    @Test
    void 상태전이가_유효하지_않으면_409를_반환한다() throws Exception {
        loginAs(1L);
        when(reviewService.getForAdmin(1L)).thenReturn(new Review(1L, "제목", "본문"));
        when(reviewService.hide(1L)).thenThrow(new BusinessException(ErrorCode.INVALID_STATE_TRANSITION));

        mockMvc.perform(post("/api/reviews/1/hide"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void 삭제에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        Review before = new Review(1L, "제목", "본문");
        Review after = new Review(1L, "제목", "본문");
        after.softDelete();
        when(reviewService.getForAdmin(1L)).thenReturn(before);
        when(reviewService.softDelete(1L)).thenReturn(after);

        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        verify(auditLogger).record(eq(1L), eq("REVIEW_DELETE"), eq("Review"), eq(1L), any(), any(), any());
    }

    @Test
    void 댓글_삭제에_성공하면_감사로그를_남긴다() throws Exception {
        loginAs(1L);

        mockMvc.perform(delete("/api/reviews/comments/5")).andExpect(status().isOk());

        verify(reviewService).softDeleteComment(5L);
        verify(auditLogger)
                .record(eq(1L), eq("REVIEW_COMMENT_DELETE"), eq("ReviewComment"), eq(5L), any(), any(), any());
    }
}
