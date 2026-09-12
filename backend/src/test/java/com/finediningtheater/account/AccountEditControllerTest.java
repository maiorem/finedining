package com.finediningtheater.account;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

// 실제 hasRole('EDITOR') 인가 검증은 EditControllerSecurityTest + 라이브 스모크 테스트에서 한다.
@WebMvcTest(AccountEditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountEditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AccountService accountService;
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
    void 회원_목록을_공개_정보만_담아_반환한다() throws Exception {
        loginAs(1L);
        Account account = new Account("kakao", "123", "user@example.com", "김아무개", SiteLocale.KO);
        when(accountService.listForAdmin()).thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nickname").value("김아무개"))
                .andExpect(jsonPath("$.data[0].provider").value("kakao"))
                .andExpect(jsonPath("$.data[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.data[0].providerUserId").doesNotExist());
    }
}
