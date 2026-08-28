package com.finediningtheater.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private MediaStorageService storageService;

    private final ImageProcessor imageProcessor = new ImageProcessor();

    private MediaService service() {
        return new MediaService(mediaAssetRepository, storageService, imageProcessor);
    }

    @Test
    void 이미지가_아닌_컨텐츠타입은_presign을_거부한다() {
        assertThatThrownBy(() -> service().presign(MediaOwnerType.PRODUCTION, 1L, 1L, "application/pdf", 1000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 파일크기가_20MB를_넘으면_presign을_거부한다() {
        long tooLarge = 21L * 1024 * 1024;

        assertThatThrownBy(() -> service().presign(MediaOwnerType.PRODUCTION, 1L, 1L, "image/jpeg", tooLarge))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 정상_요청이면_PENDING_상태의_MediaAsset을_만들고_presign_URL을_반환한다() throws MalformedURLException {
        when(mediaAssetRepository.countByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCTION, 1L)).thenReturn(0);
        when(mediaAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.presignPut(anyString(), eq("image/jpeg"), any())).thenReturn(new URL("http://localhost:9000/fdt-media-local/originals/x.jpg"));

        MediaService.PresignResult result = service().presign(MediaOwnerType.PRODUCTION, 1L, 1L, "image/jpeg", 1000);

        assertThat(result.mediaAssetId()).isNull(); // save() 목이 id를 채워주지 않으므로 null이 정상
        assertThat(result.uploadUrl()).contains("originals/");
    }

    @Test
    void 계정별_시간당_상한을_넘으면_RATE_LIMITED를_던진다() throws MalformedURLException {
        when(mediaAssetRepository.countByOwnerTypeAndOwnerId(MediaOwnerType.PRODUCTION, 1L)).thenReturn(0);
        when(mediaAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.presignPut(anyString(), anyString(), any()))
                .thenReturn(new URL("http://localhost:9000/fdt-media-local/originals/x.jpg"));

        MediaService service = service();
        for (int i = 0; i < 30; i++) {
            service.presign(MediaOwnerType.PRODUCTION, 1L, 42L, "image/jpeg", 1000);
        }

        assertThatThrownBy(() -> service.presign(MediaOwnerType.PRODUCTION, 1L, 42L, "image/jpeg", 1000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED));
    }

    @Test
    void 완료_콜백은_매직바이트_검증에_실패하면_FAILED로_기록한다() {
        MediaAsset asset = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/fake.jpg");
        when(mediaAssetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storageService.headObject("originals/fake.jpg"))
                .thenReturn(HeadObjectResponse.builder().contentLength(100L).build());
        when(storageService.getObjectRange(eq("originals/fake.jpg"), anyLong(), anyLong()))
                .thenReturn("this is not an image".getBytes());

        MediaAsset result = service().completeUpload(1L, "설명");

        assertThat(result.getStatus()).isEqualTo(MediaAssetStatus.FAILED);
        assertThat(result.getFailureReason()).isNotBlank();
    }

    @Test
    void 완료_콜백은_유효한_이미지면_파생본을_만들고_READY로_기록한다() throws Exception {
        MediaAsset asset = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/real.jpg");
        byte[] jpeg = jpegBytes(800, 400);
        when(mediaAssetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storageService.headObject("originals/real.jpg"))
                .thenReturn(HeadObjectResponse.builder().contentLength((long) jpeg.length).build());
        when(storageService.getObjectRange(eq("originals/real.jpg"), anyLong(), anyLong()))
                .thenReturn(java.util.Arrays.copyOf(jpeg, 32));
        when(storageService.getObjectBytes("originals/real.jpg")).thenReturn(jpeg);

        MediaAsset result = service().completeUpload(1L, "큐시트 사진");

        assertThat(result.getStatus()).isEqualTo(MediaAssetStatus.READY);
        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(400);
        assertThat(result.getAltText()).isEqualTo("큐시트 사진");
        verify(storageService, times(3)).putObject(anyString(), any(), eq("image/jpeg"));
    }

    @Test
    void 삭제는_모든_오브젝트키를_지우고_행을_삭제한다() {
        MediaAsset asset = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/x.jpg");
        when(mediaAssetRepository.findById(1L)).thenReturn(Optional.of(asset));

        service().delete(1L);

        verify(storageService).deleteObjects(List.of("originals/x.jpg"));
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void 발행시_READY_상태만_공개로_승격한다() {
        MediaAsset ready = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 0, "originals/a.jpg");
        ready.markReady(100, 100, "d640", "d960", "d1600", "lqip", "alt");
        MediaAsset pending = new MediaAsset(MediaOwnerType.PRODUCTION, 1L, 1, "originals/b.jpg");
        when(mediaAssetRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(MediaOwnerType.PRODUCTION, 1L))
                .thenReturn(List.of(ready, pending));

        service().publishAllFor(MediaOwnerType.PRODUCTION, 1L);

        assertThat(ready.isPublished()).isTrue();
        assertThat(pending.isPublished()).isFalse();
    }

    private byte[] jpegBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
