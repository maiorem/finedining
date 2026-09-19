package com.finediningtheater.site;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SiteController.class)
@AutoConfigureMockMvc(addFilters = false)
class SiteControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SiteVisibilityService siteVisibilityService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 공개_상태를_반환한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(true);

        mockMvc.perform(get("/api/site/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(true));
    }

    @Test
    void 비공개_상태를_반환한다() throws Exception {
        when(siteVisibilityService.isPublic()).thenReturn(false);

        mockMvc.perform(get("/api/site/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open").value(false));
    }
}
