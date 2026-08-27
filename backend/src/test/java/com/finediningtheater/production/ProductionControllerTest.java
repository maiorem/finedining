package com.finediningtheater.production;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 이 슬라이스는 공개 조회 응답 모양만 검증한다. 인증 필터 동작은
// JwtAuthenticationFilterTest·AdminAuthControllerTest에서 따로 다룬다.
@WebMvcTest(ProductionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductionService productionService;

    // 이미지 파이프라인(§7.5) 연동으로 생긴 의존성. 이 슬라이스는 응답 모양만 보므로 목록은 비워둔다.
    @MockitoBean private MediaService mediaService;

    // JwtAuthenticationFilter가 Filter로 스캔되면서 딸려오는 의존성 — 이 슬라이스에선 실행되지
    // 않지만(addFilters=false) 빈 그래프를 만족시켜야 컨텍스트가 뜬다.
    @MockitoBean private JwtProvider jwtProvider;

    @Test
    void 발행된_작품_목록을_반환한다() throws Exception {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        when(productionService.listPublished()).thenReturn(List.of(production));

        mockMvc.perform(get("/api/productions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].slug").value("showcase"))
                .andExpect(jsonPath("$.data[0].title").value("쇼케이스"));
    }

    @Test
    void 존재하지_않는_슬러그는_404와_ENTITY_NOT_FOUND를_반환한다() throws Exception {
        when(productionService.getPublished("unknown"))
                .thenThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        mockMvc.perform(get("/api/productions/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ENTITY_NOT_FOUND"));
    }
}
