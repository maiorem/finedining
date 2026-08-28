package com.finediningtheater.review;

/** 물리 삭제하지 않는다 — DELETED도 행으로 남는다 (CLAUDE.md §3.6). */
public enum ReviewStatus {
    PUBLISHED,
    HIDDEN,
    DELETED
}
