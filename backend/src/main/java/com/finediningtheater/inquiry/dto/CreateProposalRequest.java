package com.finediningtheater.inquiry.dto;

import com.finediningtheater.inquiry.ProposalCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 공개 제출 폼. website는 허니팟이다 — 사람 눈에는 안 보이지만 봇은 채운다(CLAUDE.md §7.7).
 * 카카오 로그인이 붙기 전까지는 로그인 없이 받으므로 이름·회신 이메일을 직접 받는다(§3.7).
 * category는 비즈니스 문의 유형 분류다(2026-09-04, 홈페이지 구성 요청사항 §6).
 */
public record CreateProposalRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String contactEmail,
        @NotNull ProposalCategory category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String body,
        @AssertTrue(message = "개인정보 수집에 동의해야 제출할 수 있습니다.") boolean privacyConsent,
        String website) {}
