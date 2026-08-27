package com.finediningtheater.showing;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.RateLimiter;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.showing.dto.BookingClickRequest;
import com.finediningtheater.showing.dto.ShowingResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 조회 + 예약 클릭 트래킹(CLAUDE.md §7.7의 "공개 API는 GET + 예외 두 개" 중 하나).
 * 쓰기(ShowingEditController)는 관리자 로그인 단계에서 추가한다.
 */
@RestController
@RequestMapping("/api/showings")
@RequiredArgsConstructor
public class ShowingController {

    private static final int BOOKING_CLICK_LIMIT_PER_MINUTE = 30;

    private final ShowingService showingService;
    private final RateLimiter rateLimiter;

    @GetMapping
    public ApiResponse<List<ShowingResponse>> list(
            @RequestParam(required = false) String productionSlug,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "KO") SiteLocale lang) {
        List<ShowingResponse> body =
                showingService.listPublished(productionSlug, from, to).stream()
                        .map(showing -> ShowingResponse.from(showing, lang))
                        .toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/{id}")
    public ApiResponse<ShowingResponse> detail(
            @PathVariable Long id, @RequestParam(defaultValue = "KO") SiteLocale lang) {
        Showing showing = showingService.getPublished(id);
        return ApiResponse.success(ShowingResponse.from(showing, lang));
    }

    @PostMapping("/{id}/booking-click")
    public ApiResponse<Void> trackBookingClick(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingClickRequest request,
            HttpServletRequest httpRequest) {
        String ip = ClientIp.resolve(httpRequest);
        if (!rateLimiter.tryAcquire("booking-click:" + ip, BOOKING_CLICK_LIMIT_PER_MINUTE)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        showingService.recordBookingClick(id, request);
        return ApiResponse.ok();
    }
}
