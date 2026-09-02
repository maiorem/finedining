package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.media.MediaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock private ProductionRepository productionRepository;
    @Mock private MediaService mediaService;
    private final BookingUrlValidator bookingUrlValidator = new BookingUrlValidator("booking.naver.com");

    private ProductionService productionService() {
        return new ProductionService(productionRepository, mediaService, bookingUrlValidator);
    }

    @Test
    void 목록_조회는_PUBLISHED_상태만_요청한다() {
        when(productionRepository.findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED))
                .thenReturn(List.of(new Production("showcase")));

        List<Production> result = productionService().listPublished();

        assertThat(result).hasSize(1);
        verify(productionRepository).findAllByStatusOrderByCreatedAtAsc(ContentStatus.PUBLISHED);
    }

    @Test
    void 존재하지_않거나_DRAFT인_슬러그는_ENTITY_NOT_FOUND를_던진다() {
        when(productionRepository.findBySlugAndStatus("unknown", ContentStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService().getPublished("unknown"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 슬러그가_이미_있으면_생성을_거부한다() {
        when(productionRepository.existsBySlug("showcase")).thenReturn(true);

        assertThatThrownBy(() -> productionService().create("showcase"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.DUPLICATE_SLUG));
    }

    @Test
    void 새로운_슬러그면_DRAFT_상태로_생성한다() {
        when(productionRepository.existsBySlug("showcase")).thenReturn(false);
        when(productionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Production created = productionService().create("showcase");

        assertThat(created.getSlug()).isEqualTo("showcase");
        assertThat(created.isPublished()).isFalse();
    }

    @Test
    void 임시저장은_공개본을_바꾸지_않는다() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", "부제");
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        productionService().saveDraftTranslation(1L, SiteLocale.KO, "새 제목", "새 부제", "새 설명");

        assertThat(production.titleFor(SiteLocale.KO)).isEqualTo("쇼케이스");
        ProductionTranslation ko = production.translationRowFor(SiteLocale.KO);
        assertThat(ko.effectiveTitle()).isEqualTo("새 제목");
        assertThat(ko.effectiveDescription()).isEqualTo("새 설명");
        assertThat(ko.getDescription()).isNull();
    }

    @Test
    void 한국어_제목이_없으면_발행을_거부한다() {
        Production production = new Production("showcase");
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        assertThatThrownBy(() -> productionService().publish(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 발행하면_draft가_공개본으로_승격되고_PUBLISHED가_된다() {
        Production production = new Production("showcase");
        ProductionTranslation ko = production.addTranslation(SiteLocale.KO, null, null);
        ko.updateDraft("새 제목", null);
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        Production result = productionService().publish(1L, 99L);

        assertThat(result.isPublished()).isTrue();
        assertThat(result.titleFor(SiteLocale.KO)).isEqualTo("새 제목");
        assertThat(result.getPublishedBy()).isEqualTo(99L);
    }

    @Test
    void 발행취소하면_DRAFT로_돌아가고_공개본_내용은_남는다() {
        Production production = new Production("showcase");
        production.addTranslation(SiteLocale.KO, "쇼케이스", null);
        production.publish(1L);
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        Production result = productionService().unpublish(1L);

        assertThat(result.isPublished()).isFalse();
        assertThat(result.getTranslations().get(0).getTitle()).isEqualTo("쇼케이스");
    }

    @Test
    void 허용되지_않은_호스트의_예약_URL은_거부한다() {
        assertThatThrownBy(() -> productionService().changeBookingUrl(1L, "https://evil.example.com"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 네이버_예약_호스트의_URL은_즉시_반영된다() {
        Production production = new Production("showcase");
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        Production result = productionService().changeBookingUrl(1L, "https://booking.naver.com/bizes/1");

        assertThat(result.getBookingUrl()).isEqualTo("https://booking.naver.com/bizes/1");
    }

    @Test
    void 위치_URL은_화이트리스트_검증_없이_즉시_반영된다() {
        Production production = new Production("showcase");
        when(productionRepository.findWithTranslationsById(1L)).thenReturn(Optional.of(production));

        Production result = productionService().changeLocationUrl(1L, "https://map.naver.com/p/somewhere");

        assertThat(result.getLocationUrl()).isEqualTo("https://map.naver.com/p/somewhere");
    }
}
