package com.finediningtheater.showing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 이 슬라이스는 공개 조회·클릭 트래킹 응답 모양만 검증한다. 인증 필터 동작은
// JwtAuthenticationFilterTest·AdminAuthControllerTest에서 따로 다룬다.
@WebMvcTest(ShowingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShowingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ShowingService showingService;
    @MockitoBean private RateLimiter rateLimiter;

    // JwtAuthenticationFilter가 Filter로 스캔되면서 딸려오는 의존성 — 이 슬라이스에선 실행되지
    // 않지만(addFilters=false) 빈 그래프를 만족시켜야 컨텍스트가 뜬다.
    @MockitoBean private JwtProvider jwtProvider;

    private Showing showing() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        Showing showing =
                new Showing(
                        production,
                        Instant.parse("2026-09-25T10:00:00Z"),
                        120,
                        "공연장 A",
                        "서울특별시 종로구",
                        SiteLocale.KO,
                        false);
        showing.changeBookingUrl("https://booking.naver.com/booking/13/bizes/000");
        return showing;
    }

    @Test
    void 예약_불가능한_회차는_예약_URL을_내려주지_않는다() throws Exception {
        Showing sold = showing();
        sold.changeSalesStatus(SalesStatus.SOLD_OUT);
        when(showingService.getPublished(1L)).thenReturn(sold);

        mockMvc.perform(get("/api/showings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingAvailable").value(false))
                .andExpect(jsonPath("$.data.bookingUrl").doesNotExist());
    }

    @Test
    void 예약_가능한_회차는_예약_URL을_그대로_내려준다() throws Exception {
        Showing open = showing();
        open.changeSalesStatus(SalesStatus.OPEN);
        when(showingService.listPublished(null, null, null)).thenReturn(List.of(open));

        mockMvc.perform(get("/api/showings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookingAvailable").value(true))
                .andExpect(jsonPath("$.data[0].bookingUrl").isNotEmpty());
    }

    @Test
    void 클릭_트래킹은_레이트리밋을_넘으면_429를_반환한다() throws Exception {
        when(rateLimiter.tryAcquire(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);

        mockMvc.perform(
                        post("/api/showings/1/booking-click")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void 존재하지_않는_회차의_클릭은_404를_반환한다() throws Exception {
        when(rateLimiter.tryAcquire(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);
        doThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND))
                .when(showingService)
                .recordBookingClick(any(), any());

        mockMvc.perform(
                        post("/api/showings/999/booking-click")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isNotFound());
    }
}
