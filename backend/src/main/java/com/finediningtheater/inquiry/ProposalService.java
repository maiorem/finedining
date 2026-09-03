package com.finediningtheater.inquiry;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.FailureLockout;
import com.finediningtheater.inquiry.dto.CreateProposalRequest;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 협업 제안 접수 + 관리자 검토(§3.7). 로그인 없이 받으므로 IP 기준으로 제한을 건다(2026-08-27). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private static final int MAX_DAILY_SUBMISSIONS_PER_IP = 3;

    private final ProposalRepository proposalRepository;
    private final EmailNotifier emailNotifier;

    // 회원당 제한을 걸 계정이 없어 IP 기준으로 대체했다 — FailureLockout을 "하루 3건까지"
    // 카운터로 재사용한다(로그인 잠금과 셰이프는 같지만 의미는 다르다).
    private final FailureLockout dailySubmissionLimiter =
            new FailureLockout(MAX_DAILY_SUBMISSIONS_PER_IP, Duration.ofDays(1));

    @Transactional
    public void create(CreateProposalRequest request, String ip) {
        if (request.website() != null && !request.website().isBlank()) {
            return; // 허니팟 — 봇에게는 성공처럼 보이게 조용히 무시한다(§7.7)
        }
        if (dailySubmissionLimiter.isLocked(ip)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        dailySubmissionLimiter.recordFailure(ip);

        proposalRepository.save(
                new Proposal(
                        request.name(), request.contactEmail(), request.category(), request.title(), request.body()));
    }

    public List<Proposal> listForAdmin() {
        return proposalRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 상세 조회 자체가 "읽음" 처리다 — SENT였다면 READ로 바뀐다. */
    @Transactional
    public Proposal getForAdmin(Long id) {
        Proposal proposal = findOrThrow(id);
        proposal.markRead();
        return proposal;
    }

    @Transactional
    public Proposal accept(Long id) {
        Proposal proposal = findOpenOrThrow(id);
        proposal.accept();
        emailNotifier.notifyStatusChange(proposal);
        return proposal;
    }

    @Transactional
    public Proposal decline(Long id) {
        Proposal proposal = findOpenOrThrow(id);
        proposal.decline();
        emailNotifier.notifyStatusChange(proposal);
        return proposal;
    }

    private Proposal findOrThrow(Long id) {
        return proposalRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private Proposal findOpenOrThrow(Long id) {
        Proposal proposal = findOrThrow(id);
        if (!proposal.isOpen()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        return proposal;
    }
}
