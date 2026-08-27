package com.finediningtheater.inquiry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 개인정보 3년 보관 후 삭제 (CLAUDE.md §7.7). */
@Component
@RequiredArgsConstructor
public class ProposalRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProposalRetentionScheduler.class);
    private static final long RETENTION_DAYS = 365L * 3;

    private final ProposalRepository proposalRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredProposals() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        long deleted = proposalRepository.deleteAllByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("[proposal-retention] 3년 지난 제안 {}건 삭제", deleted);
        }
    }
}
