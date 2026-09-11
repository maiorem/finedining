package com.finediningtheater.press.dto;

import com.finediningtheater.press.OgPreviewFetcher;

public record PressPreviewResponse(String title, String description, String imageUrl) {

    public static PressPreviewResponse from(OgPreviewFetcher.OgPreview preview) {
        return new PressPreviewResponse(preview.title(), preview.description(), preview.imageUrl());
    }
}
