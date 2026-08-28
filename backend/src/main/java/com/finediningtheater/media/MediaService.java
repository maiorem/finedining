package com.finediningtheater.media;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.FailureLockout;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * 이미지 업로드 파이프라인 (CLAUDE.md §7.5). presign 발급 → 브라우저가 S3/MinIO에 직접 PUT →
 * 완료 콜백에서 매직 바이트 검증 + 파생본 생성까지 이 서비스가 맡는다. 파생본 생성은 콘텐츠
 * 규모가 작으므로 비동기/폴링 없이 완료 요청 안에서 동기로 끝낸다. Production·Artist 등
 * 여러 도메인이 공유하는 범용 서비스다(§6) — ownerType+ownerId로만 대상을 구분한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // CLAUDE.md §2: 고해상도 이미지 ≤20MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Duration PRESIGN_VALIDITY = Duration.ofMinutes(10);
    private static final int MAX_PRESIGNS_PER_HOUR = 30;

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorageService storageService;
    private final ImageProcessor imageProcessor;

    // 계정당 발급 레이트리밋 (CLAUDE.md §3.5) — 스프링 빈이 아니라 이 서비스가 정책값으로 들고 있는다.
    private final FailureLockout presignRateLimiter = new FailureLockout(MAX_PRESIGNS_PER_HOUR, Duration.ofHours(1));

    public record PresignResult(Long mediaAssetId, String uploadUrl) {}

    @Transactional
    public PresignResult presign(
            MediaOwnerType ownerType, Long ownerId, Long adminId, String contentType, long contentLengthBytes) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미지 파일만 업로드할 수 있습니다.");
        }
        if (contentLengthBytes <= 0 || contentLengthBytes > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일 크기는 20MB 이하여야 합니다.");
        }
        String rateLimitKey = String.valueOf(adminId);
        if (presignRateLimiter.isLocked(rateLimitKey)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        presignRateLimiter.recordFailure(rateLimitKey);

        String key = "originals/" + UUID.randomUUID() + extensionFor(contentType);
        int sortOrder = mediaAssetRepository.countByOwnerTypeAndOwnerId(ownerType, ownerId);
        MediaAsset asset = mediaAssetRepository.save(new MediaAsset(ownerType, ownerId, sortOrder, key));

        URL uploadUrl = storageService.presignPut(key, contentType, PRESIGN_VALIDITY);
        return new PresignResult(asset.getId(), uploadUrl.toString());
    }

    /**
     * 업로드 완료 콜백. 매직 바이트로 실제 이미지인지 확인한 뒤 파생본을 만든다. 실패해도
     * 예외를 던지지 않고 FAILED 상태로 기록한다 — 운영자가 편집 화면에서 실패를 봐야 한다(§7.5).
     */
    @Transactional
    public MediaAsset completeUpload(Long id, String altText) {
        MediaAsset asset =
                mediaAssetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        try {
            HeadObjectResponse head = storageService.headObject(asset.getOriginalKey());
            if (head.contentLength() == null || head.contentLength() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalStateException("파일 크기가 20MB를 넘습니다.");
            }

            byte[] header = storageService.getObjectRange(asset.getOriginalKey(), 0, 31);
            if (!ImageMagicBytes.isValidImage(header)) {
                throw new IllegalStateException("이미지 파일이 아닙니다.");
            }

            byte[] originalBytes = storageService.getObjectBytes(asset.getOriginalKey());
            ImageProcessor.ProcessedImage processed = imageProcessor.process(originalBytes);

            String base = asset.getOriginalKey().replaceFirst("^originals/", "derivatives/").replaceFirst("\\.[^.]+$", "");
            String key640 = base + "-640.jpg";
            String key960 = base + "-960.jpg";
            String key1600 = base + "-1600.jpg";
            storageService.putObject(key640, processed.jpegDerivativesByWidth().get(640), "image/jpeg");
            storageService.putObject(key960, processed.jpegDerivativesByWidth().get(960), "image/jpeg");
            storageService.putObject(key1600, processed.jpegDerivativesByWidth().get(1600), "image/jpeg");

            asset.markReady(
                    processed.width(), processed.height(), key640, key960, key1600, processed.lqipBase64(), altText);
        } catch (IOException | RuntimeException e) {
            asset.markFailed(e.getMessage() == null ? "이미지 처리 중 오류가 발생했습니다." : e.getMessage());
        }

        return asset;
    }

    /**
     * 이미 완료된 이미지의 캡션(대체 텍스트)만 고친다 — 다시 업로드하지 않아도 되게 한다.
     * media 패키지는 소유자(Production/Artist)를 모르므로(§6), 이미지가 걸린 콘텐츠 캐시를
     * 전부 비운다 — 이미 발행된 이미지라면 캐시 무효화가 없으면 공개 화면에 반영되지 않는다
     * (ArtistService.changeLinkUrl과 같은 이유).
     */
    @Transactional
    @CacheEvict(
            value = {"productions", "productionDetail", "artists", "artistDetail"},
            allEntries = true)
    public MediaAsset updateAltText(Long id, String altText) {
        MediaAsset asset =
                mediaAssetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        asset.updateAltText(altText);
        return asset;
    }

    @Transactional
    public void delete(Long id) {
        MediaAsset asset =
                mediaAssetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        storageService.deleteObjects(asset.allObjectKeys());
        mediaAssetRepository.delete(asset);
    }

    public MediaAsset get(Long id) {
        return mediaAssetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<MediaAsset> listForAdmin(MediaOwnerType ownerType, Long ownerId) {
        return mediaAssetRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId);
    }

    public List<MediaAsset> listPublished(MediaOwnerType ownerType, Long ownerId) {
        return mediaAssetRepository.findByOwnerTypeAndOwnerIdAndPublishedTrueOrderBySortOrderAsc(ownerType, ownerId);
    }

    /** 소유자의 publish()와 같은 트랜잭션에서 호출된다 — 발행 시 READY 이미지를 함께 공개한다. */
    @Transactional
    public void publishAllFor(MediaOwnerType ownerType, Long ownerId) {
        mediaAssetRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId).stream()
                .filter(asset -> asset.getStatus() == MediaAssetStatus.READY)
                .forEach(MediaAsset::publish);
    }

    public String publicUrl(String key) {
        return storageService.publicUrl(key);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
