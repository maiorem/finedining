package com.finediningtheater.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProposalTest {

    private Proposal proposal() {
        return new Proposal("김철수", "chulsoo@example.com", "협업 제안", "본문입니다.");
    }

    @Test
    void 생성하면_SENT_상태이고_동의_시각이_기록된다() {
        Proposal proposal = proposal();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.SENT);
        assertThat(proposal.getConsentAgreedAt()).isNotNull();
        assertThat(proposal.isOpen()).isTrue();
    }

    @Test
    void SENT에서_markRead하면_READ가_된다() {
        Proposal proposal = proposal();

        proposal.markRead();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.READ);
    }

    @Test
    void READ_이후_markRead는_상태를_바꾸지_않는다() {
        Proposal proposal = proposal();
        proposal.accept();

        proposal.markRead();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
    }

    @Test
    void accept하면_ACCEPTED가_되고_더_이상_열려있지_않다() {
        Proposal proposal = proposal();

        proposal.accept();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(proposal.isOpen()).isFalse();
    }

    @Test
    void decline하면_DECLINED가_되고_더_이상_열려있지_않다() {
        Proposal proposal = proposal();

        proposal.decline();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.DECLINED);
        assertThat(proposal.isOpen()).isFalse();
    }
}
