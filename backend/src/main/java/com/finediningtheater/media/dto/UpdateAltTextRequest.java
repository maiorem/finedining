package com.finediningtheater.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// alt 텍스트는 필수다 — 업로드 시점과 같은 제약을 그대로 적용한다 (CLAUDE.md §8.8).
public record UpdateAltTextRequest(@NotBlank @Size(max = 300) String altText) {}
