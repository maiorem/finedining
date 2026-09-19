package com.finediningtheater.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaAssetStatus;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewImageServiceTest {

    @Mock private ReviewService reviewService;
    @Mock private MediaService mediaService;

    private ReviewImageService service() {
        return new ReviewImageService(reviewService, mediaService);
    }

    private MediaAsset asset(MediaOwnerType type, Long ownerId, MediaAssetStatus status) {
        MediaAsset asset = new MediaAsset(type, ownerId, 0, "originals/x.jpg");
        if (status == MediaAssetStatus.FAILED) {
            asset.markFailed("x");
        }
        return asset;
    }

    @Test
    void 남의_글이면_presign을_거부하고_발급하지_않는다() {
        when(reviewService.requireOwnedForImages(1L, 4L)).thenThrow(new BusinessException(ErrorCode.POST_NOT_OWNED));

        assertThatThrownBy(() -> service().presign(1L, 4L, "image/jpeg", 1000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.POST_NOT_OWNED));
        verify(mediaService, never()).presignForMember(MediaOwnerType.REVIEW, 1L, 4L, "image/jpeg", 1000);
    }

    @Test
    void 이미지가_3장이면_4번째_presign을_거부한다() {
        when(mediaService.listForAdmin(MediaOwnerType.REVIEW, 1L))
                .thenReturn(
                        List.of(
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.PENDING),
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.PENDING),
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.PENDING)));

        assertThatThrownBy(() -> service().presign(1L, 4L, "image/jpeg", 1000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(mediaService, never()).presignForMember(MediaOwnerType.REVIEW, 1L, 4L, "image/jpeg", 1000);
    }

    @Test
    void 실패한_업로드는_장수에서_제외한다() {
        when(mediaService.listForAdmin(MediaOwnerType.REVIEW, 1L))
                .thenReturn(
                        List.of(
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.FAILED),
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.FAILED),
                                asset(MediaOwnerType.REVIEW, 1L, MediaAssetStatus.FAILED)));
        when(mediaService.presignForMember(MediaOwnerType.REVIEW, 1L, 4L, "image/jpeg", 1000))
                .thenReturn(new MediaService.PresignResult(9L, "http://upload"));

        assertThat(service().presign(1L, 4L, "image/jpeg", 1000).mediaAssetId()).isEqualTo(9L);
    }

    @Test
    void 다른_글의_이미지_id로는_완료도_삭제도_할_수_없다() {
        when(mediaService.get(7L)).thenReturn(asset(MediaOwnerType.REVIEW, 2L, MediaAssetStatus.PENDING));

        assertThatThrownBy(() -> service().complete(1L, 4L, 7L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service().delete(1L, 4L, 7L)).isInstanceOf(BusinessException.class);
        verify(mediaService, never()).delete(7L);
    }

    @Test
    void 다른_도메인의_이미지_id로는_완료할_수_없다() {
        when(mediaService.get(7L)).thenReturn(asset(MediaOwnerType.PRODUCTION, 1L, MediaAssetStatus.PENDING));

        assertThatThrownBy(() -> service().complete(1L, 4L, 7L)).isInstanceOf(BusinessException.class);
    }
}
