package com.finediningtheater.global.security;

import com.finediningtheater.account.AdminRole;

/** {@code @AuthenticationPrincipal}로 컨트롤러에 주입되는 인증된 관리자 신원. */
public record AdminPrincipal(Long id, String username, AdminRole role) {}
