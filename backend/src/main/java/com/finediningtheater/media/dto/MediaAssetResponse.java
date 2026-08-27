package com.finediningtheater.media.dto;

import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaService;

/**
 * 공개·관리자 응답에 공용으로 쓴다. status/failureReason은 사용자별로 달라지는 값이 아니라
 * (뷰어 무관) 콘텐츠 자체의 처리 상태이므로 캐시 가능성을 해치지 않는다 (CLAUDE.md §7.2).
 */
public record MediaAssetResponse(
        Long id,
        String status,
        String failureReason,
        Integer width,
        Integer height,
        String altText,
        String lqipBase64,
        String url640,
        String url960,
        String url1600,
        boolean published) {

    public static MediaAssetResponse from(MediaAsset asset, MediaService mediaService) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getStatus().name(),
                asset.getFailureReason(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getAltText(),
                asset.getLqipBase64(),
                asset.getDerivative640Key() == null ? null : mediaService.publicUrl(asset.getDerivative640Key()),
                asset.getDerivative960Key() == null ? null : mediaService.publicUrl(asset.getDerivative960Key()),
                asset.getDerivative1600Key() == null ? null : mediaService.publicUrl(asset.getDerivative1600Key()),
                asset.isPublished());
    }
}
