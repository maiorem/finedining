package com.finediningtheater.inquiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SES/Resend 자격증명이 아직 없어 로그만 남긴다(2026-08-27, CLAUDE.md §3.7·§5). 실제 발송
 * 구현이 준비되면 이 클래스를 교체한다 — 호출부(ProposalService)는 손댈 필요 없다.
 */
@Component
public class NoOpEmailNotifier implements EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailNotifier.class);

    @Override
    public void notifyStatusChange(Proposal proposal) {
        log.info(
                "[email-noop] proposal {} 상태가 {}로 바뀜 — {}에게 알림 발송 예정(이메일 서비스 미연동)",
                proposal.getId(),
                proposal.getStatus(),
                proposal.getContactEmail());
    }
}
