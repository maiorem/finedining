package com.finediningtheater.program;

import static org.mockito.Mockito.doThrow;
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
import com.finediningtheater.global.security.SudoMode;
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

@WebMvcTest(ProgramEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProgramService programService;
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
    void 생성에_성공한다() throws Exception {
        loginAs(1L);
        when(programService.create()).thenReturn(new Program());

        mockMvc.perform(post("/api/programs")).andExpect(status().isOk());
    }

    @Test
    void 관리자_목록을_반환한다() throws Exception {
        loginAs(1L);
        when(programService.listForAdmin()).thenReturn(List.of(new Program()));

        mockMvc.perform(get("/api/programs/manage")).andExpect(status().isOk());
    }

    @Test
    void sudo가_열려있지_않으면_발행을_거부한다() throws Exception {
        loginAs(1L);
        doThrow(new BusinessException(ErrorCode.PIN_REQUIRED)).when(sudoMode).requireActive(1L);

        mockMvc.perform(post("/api/programs/1/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PIN_REQUIRED"));
    }
}
