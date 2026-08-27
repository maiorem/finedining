package com.finediningtheater.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// alt 텍스트는 필수다 — 이미지 중심 사이트에서 접근성·SEO가 같이 걸려 있다 (CLAUDE.md §8.8).
public record CompleteUploadRequest(@NotBlank @Size(max = 300) String altText) {}
