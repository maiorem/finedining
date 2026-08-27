package com.finediningtheater.showing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.showing.dto.BookingClickRequest;
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

    private ShowingService showingService() {
        return new ShowingService(showingRepository, bookingClickRepository);
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
}
