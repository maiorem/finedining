package com.finediningtheater.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.MemberPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberWithdrawalController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberWithdrawalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MemberWithdrawalService memberWithdrawalService;
    @MockitoBean private AuditLogger auditLogger;
    @MockitoBean private JwtProvider jwtProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 회원이_탈퇴하면_쿠키를_지우고_감사로그를_남긴다() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(new MemberPrincipal(4L, "손님"), null, List.of()));

        mockMvc.perform(delete("/api/auth/member/me"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("member_refresh_token", 0));

        verify(memberWithdrawalService).withdraw(4L);
        verify(auditLogger).record(eq(4L), eq("MEMBER_WITHDRAW"), eq("Account"), eq(4L), any(), any(), any());
    }

    @Test
    void 관리자_세션은_탈퇴할_수_없다() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AdminPrincipal(1L, "admin", AdminRole.SUPER_ADMIN),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

        mockMvc.perform(delete("/api/auth/member/me")).andExpect(status().isForbidden());

        verify(memberWithdrawalService, never()).withdraw(any());
    }

    @Test
    void 로그인하지_않았으면_거부한다() throws Exception {
        mockMvc.perform(delete("/api/auth/member/me")).andExpect(status().isForbidden());

        verify(memberWithdrawalService, never()).withdraw(any());
    }
}
