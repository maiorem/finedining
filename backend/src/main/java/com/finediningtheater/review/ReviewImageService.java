package com.finediningtheater.review;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaAssetStatus;
import com.finediningtheater.media.MediaOwnerType;
import com.finediningtheater.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이야기(리뷰) 이미지 첨부(2026-09-19, 최대 3장). 회원이 자기 글에만 올릴 수 있다 — 소유권과 장수
 * 상한은 여기서 서버가 강제한다(§3.3·§3.5). 파일 크기·형식·발급 횟수 상한은 MediaService가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewImageService {

    static final int MAX_IMAGES = 3;
    // 회원 첨부는 alt 입력을 받지 않는다 — 운영자가 모더레이션 화면에서 볼 수 있는 최소 대체 텍스트(§8.8).
    static final String DEFAULT_ALT = "이야기 첨부 이미지";

    private final ReviewService reviewService;
    private final MediaService mediaService;

    @Transactional
    public MediaService.PresignResult presign(Long reviewId, Long accountId, String contentType, long contentLengthBytes) {
        reviewService.requireOwnedForImages(reviewId, accountId);
        // 실패한 업로드는 장수에서 뺀다 — 그래야 실패했다고 슬롯이 영영 막히지 않는다.
        long used =
                mediaService.listForAdmin(MediaOwnerType.REVIEW, reviewId).stream()
                        .filter(asset -> asset.getStatus() != MediaAssetStatus.FAILED)
                        .count();
        if (used >= MAX_IMAGES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미지는 최대 " + MAX_IMAGES + "장까지 첨부할 수 있습니다.");
        }
        return mediaService.presignForMember(MediaOwnerType.REVIEW, reviewId, accountId, contentType, contentLengthBytes);
    }

    @Transactional
    public MediaAsset complete(Long reviewId, Long accountId, Long mediaId) {
        reviewService.requireOwnedForImages(reviewId, accountId);
        requireBelongsToReview(mediaService.get(mediaId), reviewId);
        return mediaService.completeMemberUpload(mediaId, DEFAULT_ALT);
    }

    @Transactional
    public void delete(Long reviewId, Long accountId, Long mediaId) {
        reviewService.requireOwnedForImages(reviewId, accountId);
        requireBelongsToReview(mediaService.get(mediaId), reviewId);
        mediaService.delete(mediaId);
    }

    // 다른 글(또는 다른 도메인)의 이미지 id를 끼워 넣어 남의 파일을 건드리지 못하게 한다.
    private void requireBelongsToReview(MediaAsset asset, Long reviewId) {
        if (asset.getOwnerType() != MediaOwnerType.REVIEW || !asset.getOwnerId().equals(reviewId)) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }
}
