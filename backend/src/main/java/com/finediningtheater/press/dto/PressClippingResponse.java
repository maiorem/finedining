package com.finediningtheater.press.dto;

import com.finediningtheater.media.MediaAsset;
import com.finediningtheater.media.MediaService;
import com.finediningtheater.press.PressClipping;

/** 공개 응답 — 뷰어 무관 데이터라 캐시 가능하다(CLAUDE.md §7.2). */
public record PressClippingResponse(Long id, String title, String externalUrl, String imageUrl, String imageAlt) {

    public static PressClippingResponse from(PressClipping clipping, MediaAsset image, MediaService mediaService) {
        return new PressClippingResponse(
                clipping.getId(),
                clipping.getTitle(),
                clipping.getExternalUrl(),
                resolveImageUrl(image, mediaService),
                image == null ? null : image.getAltText());
    }

    private static String resolveImageUrl(MediaAsset image, MediaService mediaService) {
        if (image == null) return null;
        String key =
                image.getDerivative1600Key() != null
                        ? image.getDerivative1600Key()
                        : image.getDerivative960Key() != null ? image.getDerivative960Key() : image.getDerivative640Key();
        return key == null ? null : mediaService.publicUrl(key);
    }
}
