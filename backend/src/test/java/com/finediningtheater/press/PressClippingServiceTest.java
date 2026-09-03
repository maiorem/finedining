package com.finediningtheater.press;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.ContentStatus;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PressClippingServiceTest {

    @Mock private PressClippingRepository pressClippingRepository;
    @Mock private MediaService mediaService;

    private PressClippingService service() {
        return new PressClippingService(pressClippingRepository, mediaService);
    }

    @Test
    void 목록_조회는_PUBLISHED_상태만_요청한다() {
        when(pressClippingRepository.findAllByStatusOrderByCreatedAtDesc(ContentStatus.PUBLISHED))
                .thenReturn(List.of(new PressClipping("제목", "https://example.com")));

        List<PressClipping> result = service().listPublished();

        assertThat(result).hasSize(1);
    }

    @Test
    void create는_DRAFT_상태로_저장한다() {
        when(pressClippingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PressClipping result = service().create("제목", "https://example.com");

        assertThat(result.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("제목");
    }

    @Test
    void updateContent은_제목과_링크를_바꾼다() {
        PressClipping clipping = new PressClipping("원래 제목", "https://example.com/old");
        when(pressClippingRepository.findById(1L)).thenReturn(Optional.of(clipping));

        PressClipping result = service().updateContent(1L, "새 제목", "https://example.com/new");

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getExternalUrl()).isEqualTo("https://example.com/new");
    }

    @Test
    void publish하면_이미지도_함께_공개로_승격한다() {
        PressClipping clipping = new PressClipping("제목", "https://example.com");
        when(pressClippingRepository.findById(1L)).thenReturn(Optional.of(clipping));

        Long id = 1L;
        PressClipping result = service().publish(id, 42L);

        assertThat(result.isPublished()).isTrue();
        verify(mediaService).publishAllFor(MediaOwnerType.PRESS_CLIPPING, id);
    }

    @Test
    void 존재하지_않는_보도자료는_ENTITY_NOT_FOUND를_던진다() {
        when(pressClippingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getForAdmin(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }
}
