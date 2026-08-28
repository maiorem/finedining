package com.finediningtheater.showing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.production.Production;
import com.finediningtheater.production.ProductionRepository;
import com.finediningtheater.showing.dto.BookingClickRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowingServiceTest {

    @Mock private ShowingRepository showingRepository;
    @Mock private BookingClickRepository bookingClickRepository;
    @Mock private ProductionRepository productionRepository;

    private static final BookingUrlValidator ALLOW_NAVER = new BookingUrlValidator("booking.naver.com");

    private ShowingService showingService() {
        return new ShowingService(showingRepository, bookingClickRepository, productionRepository, ALLOW_NAVER);
    }

    @Test
    void 작품_슬러그로_조회하면_PUBLISHED_상태만_요청한다() {
        when(showingRepository.findByStatusAndProduction_SlugOrderByStartsAtAsc(
                        ContentStatus.PUBLISHED, "showcase"))
                .thenReturn(List.of());

        showingService().listPublished("showcase", null, null);

        verify(showingRepository)
                .findByStatusAndProduction_SlugOrderByStartsAtAsc(ContentStatus.PUBLISHED, "showcase");
    }

    @Test
    void 존재하지_않거나_DRAFT인_회차는_ENTITY_NOT_FOUND를_던진다() {
        when(showingRepository.findByIdAndStatus(99L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> showingService().getPublished(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 존재하지_않는_회차의_클릭은_기록하지_않고_예외를_던진다() {
        when(showingRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> showingService().recordBookingClick(1L, null))
                .isInstanceOf(BusinessException.class);
        verify(bookingClickRepository, never()).save(any());
    }

    @Test
    void 요청_본문이_없어도_클릭_자체는_기록한다() {
        when(showingRepository.existsById(1L)).thenReturn(true);

        showingService().recordBookingClick(1L, null);

        verify(bookingClickRepository).save(any());
    }

    @Test
    void 요청에_담긴_유입_정보가_그대로_저장된다() {
        when(showingRepository.existsById(1L)).thenReturn(true);
        BookingClickRequest request = new BookingClickRequest("hero", "ko", "naver", "cpc", "launch");

        showingService().recordBookingClick(1L, request);

        verify(bookingClickRepository)
                .save(
                        argThat(
                                click ->
                                        click.getShowingId().equals(1L)
                                                && click.getChannel().equals("hero")
                                                && click.getUtmCampaign().equals("launch")));
    }

    @Test
    void 존재하지_않는_작품으로는_회차를_만들_수_없다() {
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                showingService()
                                        .create(1L, Instant.now(), 120, "장소", null, SiteLocale.KO, false))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    void 존재하는_작품으로_회차를_만든다() {
        Production production = new Production("showcase");
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));
        when(showingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Showing created =
                showingService().create(1L, Instant.parse("2026-09-21T10:00:00Z"), 120, "장소", "주소", SiteLocale.KO, true);

        assertThat(created.getProduction()).isSameAs(production);
        assertThat(created.getVenueName()).isEqualTo("장소");
        assertThat(created.isPublished()).isFalse();
    }

    @Test
    void 허용되지_않은_호스트로_예약_URL을_바꾸면_거부한다() {
        // 검증은 회차를 조회하기 전에 먼저 실패하므로 repository는 호출되지 않는다.
        assertThatThrownBy(() -> showingService().changeBookingUrl(1L, "https://evil.example.com/booking"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 허용된_호스트면_예약_URL이_바뀐다() {
        Showing showing = new Showing(new Production("showcase"), Instant.now(), 120, "장소", null, SiteLocale.KO, false);
        when(showingRepository.findWithProductionById(1L)).thenReturn(Optional.of(showing));

        Showing result = showingService().changeBookingUrl(1L, "https://booking.naver.com/bizes/1/items/1");

        assertThat(result.getBookingUrl()).isEqualTo("https://booking.naver.com/bizes/1/items/1");
    }

    @Test
    void 발행하면_PUBLISHED_상태가_된다() {
        Showing showing = new Showing(new Production("showcase"), Instant.now(), 120, "장소", null, SiteLocale.KO, false);
        when(showingRepository.findWithProductionById(1L)).thenReturn(Optional.of(showing));

        Showing result = showingService().publish(1L, 99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.getPublishedBy()).isEqualTo(99L);
    }
}
