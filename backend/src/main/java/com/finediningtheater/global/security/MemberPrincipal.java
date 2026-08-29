package com.finediningtheater.global.security;

/** {@code @AuthenticationPrincipal}로 컨트롤러에 주입되는 인증된 일반 회원 신원. role이 없다
 * — 로그인 자체가 곧 리뷰 작성 자격이다(CLAUDE.md §3.1). */
public record MemberPrincipal(Long id, String nickname) {}
