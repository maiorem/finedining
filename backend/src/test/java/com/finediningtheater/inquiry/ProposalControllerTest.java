package com.finediningtheater.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.inquiry.dto.CreateProposalRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProposalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProposalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProposalService proposalService;
    @MockitoBean private RateLimiter rateLimiter;
    @MockitoBean private JwtProvider jwtProvider;

    private static final String VALID_BODY =
            "{\"name\":\"김철수\",\"contactEmail\":\"chulsoo@example.com\",\"category\":\"CORPORATE_EVENT\","
                    + "\"title\":\"제목\",\"body\":\"본문\",\"privacyConsent\":true}";

    @Test
    void 정상_제출이면_200을_반환한다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(proposalService).create(any(), any());
    }

    @Test
    void 개인정보_동의가_없으면_검증_오류를_반환한다() throws Exception {
        String body =
                "{\"name\":\"김철수\",\"contactEmail\":\"chulsoo@example.com\",\"category\":\"CORPORATE_EVENT\","
                        + "\"title\":\"제목\",\"body\":\"본문\",\"privacyConsent\":false}";

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(proposalService, never()).create(any(), any());
    }

    @Test
    void 이메일_형식이_틀리면_검증_오류를_반환한다() throws Exception {
        String body =
                "{\"name\":\"김철수\",\"contactEmail\":\"not-an-email\",\"category\":\"CORPORATE_EVENT\","
                        + "\"title\":\"제목\",\"body\":\"본문\",\"privacyConsent\":true}";

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 카테고리가_없으면_검증_오류를_반환한다() throws Exception {
        String body =
                "{\"name\":\"김철수\",\"contactEmail\":\"chulsoo@example.com\","
                        + "\"title\":\"제목\",\"body\":\"본문\",\"privacyConsent\":true}";

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(proposalService, never()).create(any(), any());
    }

    @Test
    void 레이트리밋을_넘으면_429를_반환하고_서비스를_호출하지_않는다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));

        verify(proposalService, never()).create(any(), any());
    }

    @Test
    void 요청_본문이_CreateProposalRequest로_매핑된다() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);

        mockMvc.perform(post("/api/proposals").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateProposalRequest> captor = ArgumentCaptor.forClass(CreateProposalRequest.class);
        verify(proposalService).create(captor.capture(), any());
        assertThat(captor.getValue().name()).isEqualTo("김철수");
        assertThat(captor.getValue().contactEmail()).isEqualTo("chulsoo@example.com");
        assertThat(captor.getValue().category()).isEqualTo(ProposalCategory.CORPORATE_EVENT);
    }
}
