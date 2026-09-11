package com.finediningtheater.press.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** ogImageUrl은 "미리보기 가져오기"로 확인한 기사 이미지 URL — 없으면(null) 기존처럼 수동
 * 업로드로 이미지를 붙인다. null이면 검증을 건너뛴다(Bean Validation 기본 동작). */
public record CreatePressClippingRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank
                @Size(max = 500)
                @Pattern(regexp = "^https?://.+", message = "http(s):// 로 시작하는 링크를 입력해 주세요.")
                String externalUrl,
        @Size(max = 500)
                @Pattern(regexp = "^https?://.+", message = "http(s):// 로 시작하는 링크를 입력해 주세요.")
                String ogImageUrl) {}
