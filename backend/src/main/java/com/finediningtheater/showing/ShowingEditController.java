package com.finediningtheater.showing;

import com.finediningtheater.global.audit.AuditLogger;
import com.finediningtheater.global.response.ApiResponse;
import com.finediningtheater.global.security.AdminPrincipal;
import com.finediningtheater.global.security.SudoMode;
import com.finediningtheater.global.support.ClientIp;
import com.finediningtheater.showing.dto.ChangeBookingUrlRequest;
import com.finediningtheater.showing.dto.ChangeSalesStatusRequest;
import com.finediningtheater.showing.dto.CreateShowingRequest;
import com.finediningtheater.showing.dto.ShowingAdminResponse;
import com.finediningtheater.showing.dto.UpdateShowingDetailsRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회차 편집(우선순위 6단계: 예약 관리, CLAUDE.md §4). 예약 URL 변경·발행·발행취소는
 * §3.4의 PIN 필수 목록에 있어 sudo 모드를 요구한다. 판매 상태 토글은 "1클릭"이 설계 의도라
 * PIN을 요구하지 않는다. 모든 쓰기에 감사 로그를 남긴다(§7.7).
 */
@RestController
@RequestMapping("/api/showings")
@PreAuthorize("hasRole('EDITOR')")
@RequiredArgsConstructor
public class ShowingEditController {

    private final ShowingService showingService;
    private final AuditLogger auditLogger;
    private final SudoMode sudoMode;

    @GetMapping("/manage")
    public ApiResponse<List<ShowingAdminResponse>> listForAdmin() {
        List<ShowingAdminResponse> body =
                showingService.listForAdmin().stream().map(ShowingAdminResponse::from).toList();
        return ApiResponse.success(body);
    }

    @GetMapping("/manage/{id}")
    public ApiResponse<ShowingAdminResponse> getForAdmin(@PathVariable Long id) {
        return ApiResponse.success(ShowingAdminResponse.from(showingService.getForAdmin(id)));
    }

    @PostMapping
    public ApiResponse<ShowingAdminResponse> create(
            @Valid @RequestBody CreateShowingRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        Showing showing =
                showingService.create(
                        request.productionId(),
                        request.startsAt(),
                        request.durationMinutes(),
                        request.venueName(),
                        request.venueAddress(),
                        request.spokenLanguage(),
                        request.interpretationAvailable());

        auditLogger.record(
                principal.id(),
                "SHOWING_CREATE",
                "Showing",
                showing.getId(),
                null,
                Map.of("productionId", request.productionId(), "startsAt", request.startsAt().toString()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(showing));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShowingAdminResponse> updateDetails(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShowingDetailsRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        Showing before = showingService.getForAdmin(id);
        Map<String, String> beforeSnapshot =
                Map.of(
                        "startsAt", before.getStartsAt().toString(),
                        "venueName", before.getVenueName());

        Showing after =
                showingService.updateDetails(
                        id,
                        request.startsAt(),
                        request.durationMinutes(),
                        request.venueName(),
                        request.venueAddress(),
                        request.spokenLanguage(),
                        request.interpretationAvailable());

        auditLogger.record(
                principal.id(),
                "SHOWING_UPDATE_DETAILS",
                "Showing",
                id,
                beforeSnapshot,
                Map.of("startsAt", after.getStartsAt().toString(), "venueName", after.getVenueName()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(after));
    }

    /** 판매 상태 1클릭 토글 — 자주 바뀌는 운영 데이터라 PIN을 요구하지 않는다(§3.4). */
    @PostMapping("/{id}/sales-status")
    public ApiResponse<ShowingAdminResponse> changeSalesStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeSalesStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        String beforeStatus = showingService.getForAdmin(id).getSalesStatus().name();
        Showing after = showingService.changeSalesStatus(id, request.salesStatus());

        auditLogger.record(
                principal.id(),
                "SHOWING_SALES_STATUS_CHANGE",
                "Showing",
                id,
                Map.of("salesStatus", beforeStatus),
                Map.of("salesStatus", after.getSalesStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(after));
    }

    /** 예약 URL 변경 — 파괴적·공개적 동작이라 PIN sudo 모드가 열려 있어야 한다(§3.4). */
    @PostMapping("/{id}/booking-url")
    public ApiResponse<ShowingAdminResponse> changeBookingUrl(
            @PathVariable Long id,
            @Valid @RequestBody ChangeBookingUrlRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeUrl = String.valueOf(showingService.getForAdmin(id).getBookingUrl());
        Showing after = showingService.changeBookingUrl(id, request.bookingUrl());

        auditLogger.record(
                principal.id(),
                "SHOWING_BOOKING_URL_CHANGE",
                "Showing",
                id,
                Map.of("bookingUrl", beforeUrl),
                Map.of("bookingUrl", String.valueOf(after.getBookingUrl())),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(after));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ShowingAdminResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = showingService.getForAdmin(id).getStatus().name();
        Showing after = showingService.publish(id, principal.id());

        auditLogger.record(
                principal.id(),
                "SHOWING_PUBLISH",
                "Showing",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(after));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ShowingAdminResponse> unpublish(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest httpRequest) {
        sudoMode.requireActive(principal.id());

        String beforeStatus = showingService.getForAdmin(id).getStatus().name();
        Showing after = showingService.unpublish(id);

        auditLogger.record(
                principal.id(),
                "SHOWING_UNPUBLISH",
                "Showing",
                id,
                Map.of("status", beforeStatus),
                Map.of("status", after.getStatus().name()),
                ClientIp.resolve(httpRequest));

        return ApiResponse.success(ShowingAdminResponse.from(after));
    }
}
