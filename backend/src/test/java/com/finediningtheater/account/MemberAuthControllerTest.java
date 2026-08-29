package com.finediningtheater.account;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 실제 보안 필터 체인 검증은 JwtAuthenticationFilterTest에서 한다. 여기서는 컨트롤러의
// 요청/응답 매핑과 쿠키 발급만 확인한다.
@WebMvcTest(MemberAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberAuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MemberAuthService memberAuthService;

    // JwtAuthenticationFilter가 Filter로 스캔되면서 딸려오는 의존성 — 이 슬라이스에선 실행되지
    // 않지만(addFilters=false) 빈 그래프를 만족시켜야 컨텍스트가 뜬다.
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void refresh_쿠키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/member/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_refresh_쿠키면_새_토큰을_돌려준다() throws Exception {
        when(memberAuthService.refresh("valid-refresh-token"))
                .thenReturn(new MemberSession("new-access-token", "new-refresh-token", "김아무개"));

        mockMvc.perform(
                        post("/api/auth/member/refresh")
                                .cookie(new Cookie("member_refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.nickname").value("김아무개"))
                .andExpect(cookie().value("member_refresh_token", "new-refresh-token"));
    }

    @Test
    void 로그아웃하면_refresh_쿠키를_만료시킨다() throws Exception {
        mockMvc.perform(post("/api/auth/member/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("member_refresh_token", 0));
    }
}
