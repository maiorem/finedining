package com.finediningtheater.media.dto;

import jakarta.validation.constraints.Size;

// 설명 문단은 alt 텍스트와 달리 선택 사항이다 — 비워서 지울 수도 있으므로 @NotBlank를 붙이지 않는다.
public record UpdateCaptionRequest(@Size(max = 2000) String caption) {}
