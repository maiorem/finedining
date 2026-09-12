package com.finediningtheater.account.dto;

import com.finediningtheater.account.Account;
import com.finediningtheater.account.AccountStatus;
import com.finediningtheater.global.support.SiteLocale;
import java.time.Instant;

/**
 * 관리자 회원 목록에 노출하는 "공개 정보"만 담는다 — email·providerUserId는 회원이 공개적으로
 * 드러낸 적 없는 개인정보라 여기 넣지 않는다(CLAUDE.md §3.2·§7.7).
 */
public record AccountAdminResponse(
        Long id, String nickname, String provider, SiteLocale locale, AccountStatus status, Instant createdAt) {

    public static AccountAdminResponse from(Account account) {
        return new AccountAdminResponse(
                account.getId(),
                account.getNickname(),
                account.getProvider(),
                account.getLocale(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
