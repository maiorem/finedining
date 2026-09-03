package com.finediningtheater.press.dto;

import com.finediningtheater.media.dto.MediaAssetResponse;
import com.finediningtheater.press.PressClipping;
import java.util.List;

public record PressClippingAdminResponse(
        Long id, String title, String externalUrl, String status, List<MediaAssetResponse> images) {

    public static PressClippingAdminResponse from(PressClipping clipping, List<MediaAssetResponse> images) {
        return new PressClippingAdminResponse(
                clipping.getId(), clipping.getTitle(), clipping.getExternalUrl(), clipping.getStatus().name(), images);
    }
}
