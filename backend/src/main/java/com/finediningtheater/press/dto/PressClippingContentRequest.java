package com.finediningtheater.press.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 등록·수정이 공유하는 요청 모양 — 언론사 도메인이 제각각이라 화이트리스트는 두지 않고
 * http(s) 스킴만 강제한다(javascript: 같은 스킴을 통한 XSS 방지). */
public record PressClippingContentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank
                @Size(max = 500)
                @Pattern(regexp = "^https?://.+", message = "http(s):// 로 시작하는 링크를 입력해 주세요.")
                String externalUrl) {}
