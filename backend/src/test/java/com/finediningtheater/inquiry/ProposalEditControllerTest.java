package com.finediningtheater.inquiry;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 실제 인가 필터 검증은 EditControllerSecurityTest + 라이브 스모크 테스트에서 한다.
@WebMvcTest(ProposalEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProposalEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProposalService proposalService;
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

    private Proposal proposal() {
        return new Proposal("김철수", "chulsoo@example.com", ProposalCategory.CORPORATE_EVENT, "제목", "본문");
    }

    @Test
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(proposalService.listForAdmin()).thenReturn(List.of(proposal()));

        mockMvc.perform(get("/api/proposals/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("김철수"));
    }

    @Test
    void 상세_조회에_성공한다() throws Exception {
        loginAs(1L);
        when(proposalService.getForAdmin(1L)).thenReturn(proposal());

        mockMvc.perform(get("/api/proposals/manage/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactEmail").value("chulsoo@example.com"));
    }

    @Test
    void 수락하면_200과_함께_감사로그를_남긴다() throws Exception {
        loginAs(1L);
        Proposal before = proposal();
        Proposal after = proposal();
        after.accept();
        when(proposalService.getForAdmin(1L)).thenReturn(before);
        when(proposalService.accept(1L)).thenReturn(after);

        mockMvc.perform(post("/api/proposals/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        verify(auditLogger)
                .record(eq(1L), eq("PROPOSAL_ACCEPT"), eq("Proposal"), eq(1L), any(), any(), any());
    }

    @Test
    void 이미_처리된_제안을_거절하면_409를_반환한다() throws Exception {
        loginAs(1L);
        when(proposalService.getForAdmin(1L)).thenReturn(proposal());
        when(proposalService.decline(1L)).thenThrow(new BusinessException(ErrorCode.INVALID_STATE_TRANSITION));

        mockMvc.perform(post("/api/proposals/1/decline"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
    }
}
