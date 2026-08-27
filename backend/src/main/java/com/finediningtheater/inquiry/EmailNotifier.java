package com.finediningtheater.inquiry;

/** 제안 상태 변경 시 발송자에게 보내는 이메일 알림 (CLAUDE.md §3.7). */
public interface EmailNotifier {

    void notifyStatusChange(Proposal proposal);
}
