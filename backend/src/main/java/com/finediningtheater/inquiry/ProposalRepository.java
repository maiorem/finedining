package com.finediningtheater.inquiry;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findAllByOrderByCreatedAtDesc();

    // 개인정보 3년 보관 후 삭제 (CLAUDE.md §7.7).
    long deleteAllByCreatedAtBefore(Instant cutoff);
}
