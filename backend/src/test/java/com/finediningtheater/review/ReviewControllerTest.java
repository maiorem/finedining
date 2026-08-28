package com.finediningtheater.review;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 공개된_리뷰_목록을_반환한다() throws Exception {
        when(reviewService.listPublished()).thenReturn(List.of(new Review(1L, "제목", "본문")));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("제목"));
    }

    @Test
    void 숨겨진_리뷰_상세는_404다() throws Exception {
        when(reviewService.getPublished(1L)).thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
    }
}
