package com.finediningtheater.account;

/** 관리자 계정의 단일 역할값. Set이 아니다 — 일반 회원(Account)과 완전히 분리된 별개 계정이라
 * "일반회원이면서 에디터"인 사람 자체가 존재하지 않는다 (CLAUDE.md §3.1, 2026-08-27 결정). */
public enum AdminRole {
    EDITOR,
    SUPER_ADMIN
}
