package com.finediningtheater.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 리뷰 작성(회원 본인)·본문 수정(회원 본인·관리자)이 공유하는 요청 모양(§3.6). */
public record ReviewContentRequest(
        @NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 4000) String body) {}
