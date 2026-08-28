package com.finediningtheater.about.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "임시저장" 페이로드. 발행 전까지는 공개본에 영향을 주지 않는다 (CLAUDE.md §3.9). */
public record UpsertAboutTranslationRequest(@NotBlank @Size(max = 4000) String intro) {}
