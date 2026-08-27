package com.finediningtheater.account;

/** 관리자는 탈퇴 개념이 없다 — SUPER_ADMIN이 비활성화한다 (CLAUDE.md §3.1). */
public enum AdminAccountStatus {
    ACTIVE,
    DISABLED
}
