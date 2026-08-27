package com.finediningtheater.showing;

/** 회차 판매 상태 배지. 잔여석 숫자는 어디에도 표시하지 않는다 (CLAUDE.md §4). */
public enum SalesStatus {
    OPEN,
    CLOSING_SOON,
    SOLD_OUT,
    ENDED
}
