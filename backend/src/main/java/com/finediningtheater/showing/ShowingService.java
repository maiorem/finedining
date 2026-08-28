package com.finediningtheater.showing;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionRepository;
import com.finediningtheater.showing.dto.BookingClickRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 조회 + 예약 클릭 트래킹 + 회차 편집(우선순위 6단계: 예약 관리). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowingService {

    // 회차 시각은 UTC로 저장하지만 "어느 날짜"는 한국 시간 기준이다 (CLAUDE.md §13.4).
    private static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Seoul");
    private static final BookingClickRequest EMPTY_CLICK =
            new BookingClickRequest(null, null, null, null, null);

    private final ShowingRepository showingRepository;
    private final BookingClickRepository bookingClickRepository;
    private final ProductionRepository productionRepository;
    private final BookingUrlValidator bookingUrlValidator;

    public List<Showing> listPublished(String productionSlug, LocalDate from, LocalDate to) {
        if (productionSlug != null && !productionSlug.isBlank()) {
            return showingRepository.findByStatusAndProduction_SlugOrderByStartsAtAsc(
                    ContentStatus.PUBLISHED, productionSlug);
        }

        LocalDate start = from != null ? from : LocalDate.now(CALENDAR_ZONE);
        LocalDate end = to != null ? to : start.plusMonths(1);
        Instant fromInstant = start.atStartOfDay(CALENDAR_ZONE).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(CALENDAR_ZONE).toInstant();
        return showingRepository.findByStatusAndStartsAtBetweenOrderByStartsAtAsc(
                ContentStatus.PUBLISHED, fromInstant, toInstant);
    }

    public Showing getPublished(Long id) {
        return showingRepository
                .findByIdAndStatus(id, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Transactional
    public void recordBookingClick(Long showingId, BookingClickRequest request) {
        if (!showingRepository.existsById(showingId)) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }
        BookingClickRequest safe = request != null ? request : EMPTY_CLICK;
        bookingClickRepository.save(
                new BookingClick(
                        showingId,
                        safe.channel(),
                        safe.locale(),
                        safe.utmSource(),
                        safe.utmMedium(),
                        safe.utmCampaign()));
    }

    // --- 관리자 (§4·§3.9) ---

    public List<Showing> listForAdmin() {
        return showingRepository.findAllByOrderByStartsAtAsc();
    }

    /** 관리자용. 상태 무관 — DRAFT도 봐야 편집할 수 있다. */
    public Showing getForAdmin(Long id) {
        return showingRepository
                .findWithProductionById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Transactional
    public Showing create(
            Long productionId,
            Instant startsAt,
            int durationMinutes,
            String venueName,
            String venueAddress,
            SiteLocale spokenLanguage,
            boolean interpretationAvailable) {
        // 응답 매핑(ShowingAdminResponse)이 컨트롤러에서 production.titleFor()를 호출하므로
        // translations를 미리 fetch join해야 한다 — 그냥 findById면 세션이 닫힌 뒤
        // LazyInitializationException이 난다.
        Production production =
                productionRepository
                        .findWithTranslationsById(productionId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        Showing showing =
                new Showing(
                        production, startsAt, durationMinutes, venueName, venueAddress, spokenLanguage, interpretationAvailable);
        return showingRepository.save(showing);
    }

    @Transactional
    public Showing updateDetails(
            Long id,
            Instant startsAt,
            int durationMinutes,
            String venueName,
            String venueAddress,
            SiteLocale spokenLanguage,
            boolean interpretationAvailable) {
        Showing showing = getForAdmin(id);
        showing.updateDetails(startsAt, durationMinutes, venueName, venueAddress, spokenLanguage, interpretationAvailable);
        return showing;
    }

    /** 판매 상태 1클릭 토글 — 자주 바뀌는 운영 데이터라 PIN을 요구하지 않는다(§3.4에 없음). */
    @Transactional
    public Showing changeSalesStatus(Long id, SalesStatus salesStatus) {
        Showing showing = getForAdmin(id);
        showing.changeSalesStatus(salesStatus);
        return showing;
    }

    /**
     * 예약 URL 변경 — 오타 하나로 예약이 통째로 죽으므로 호스트 화이트리스트로 검증한다(§4).
     * PIN sudo 모드 확인은 컨트롤러에서 한다(§3.4 "예약 URL 변경"이 명시된 목록).
     */
    @Transactional
    public Showing changeBookingUrl(Long id, String bookingUrl) {
        bookingUrlValidator.validate(bookingUrl);
        Showing showing = getForAdmin(id);
        showing.changeBookingUrl(bookingUrl);
        return showing;
    }

    @Transactional
    public Showing publish(Long id, Long adminId) {
        Showing showing = getForAdmin(id);
        showing.publish(adminId);
        return showing;
    }

    @Transactional
    public Showing unpublish(Long id) {
        Showing showing = getForAdmin(id);
        showing.unpublish();
        return showing;
    }
}
