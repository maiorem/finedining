package com.finediningtheater.press.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PressPreviewRequest(
        @NotBlank
                @Size(max = 500)
                @Pattern(regexp = "^https?://.+", message = "http(s):// 로 시작하는 링크를 입력해 주세요.")
                String url) {}
