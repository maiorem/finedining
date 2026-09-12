package com.finediningtheater.account.dto;

import com.finediningtheater.account.Account;
import com.finediningtheater.account.AccountStatus;
import com.finediningtheater.global.support.SiteLocale;
import java.time.Instant;

/**
 * 관리자 회원 목록에 노출하는 정보를 담는다. providerUserId(카카오 내부 식별자)는 회원이
 * 공개적으로 드러낸 적 없는 값이라 여전히 뺀다(CLAUDE.md §3.2·§7.7). email은 2026-09-12
 * 결정으로 포함했다 — 개인정보처리방침에 "관리자의 회원관리 열람" 목적을 명시하는 작업이
 * 별도로 필요하다(§15, 오픈 전 필수 문서 작업).
 */
public record AccountAdminResponse(
        Long id,
        String nickname,
        String email,
        String provider,
        SiteLocale locale,
        AccountStatus status,
        Instant createdAt) {

    public static AccountAdminResponse from(Account account) {
        return new AccountAdminResponse(
                account.getId(),
                account.getNickname(),
                account.getEmail(),
                account.getProvider(),
                account.getLocale(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
