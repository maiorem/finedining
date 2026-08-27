package com.finediningtheater.inquiry;

/** SENT → READ → ACCEPTED | DECLINED (CLAUDE.md §3.7). ACCEPTED/DECLINED는 종결 상태다. */
public enum ProposalStatus {
    SENT,
    READ,
    ACCEPTED,
    DECLINED
}
