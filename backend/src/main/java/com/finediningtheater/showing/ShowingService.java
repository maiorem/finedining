package com.finediningtheater.showing;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.showing.dto.BookingClickRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 조회 + 예약 클릭 트래킹만 다룬다. 쓰기(ShowingEditController)는 관리자 로그인 단계에서 추가한다. */
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
}
