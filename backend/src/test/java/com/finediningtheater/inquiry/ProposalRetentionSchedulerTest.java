package com.finediningtheater.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalRetentionSchedulerTest {

    @Mock private ProposalRepository proposalRepository;

    @Test
    void 삼년보다_오래된_제안만_삭제_대상으로_넘긴다() {
        when(proposalRepository.deleteAllByCreatedAtBefore(any())).thenReturn(0L);

        new ProposalRetentionScheduler(proposalRepository).deleteExpiredProposals();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(proposalRepository).deleteAllByCreatedAtBefore(captor.capture());

        Instant cutoff = captor.getValue();
        Instant expectedApprox = Instant.now().minusSeconds(365L * 3 * 24 * 3600);
        // 정확히 같은 나노초일 필요는 없다 — 3년 전후 1분 오차는 허용한다.
        assertThat(cutoff).isCloseTo(expectedApprox, within(1, ChronoUnit.MINUTES));
    }
}
