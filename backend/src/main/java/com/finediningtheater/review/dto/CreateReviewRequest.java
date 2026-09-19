package com.finediningtheater.review.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원 본인 작성. 이름·연락처는 감사 리워드 연락용이고 개인정보 수집 동의가 필수다(2026-09-19). */
public record CreateReviewRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String body,
        @NotBlank @Size(max = 50) String authorName,
        @Size(max = 100) String contact,
        @AssertTrue(message = "개인정보 수집·이용에 동의해 주세요.") boolean privacyConsent) {}
