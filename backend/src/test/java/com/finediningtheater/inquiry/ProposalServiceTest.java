package com.finediningtheater.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.inquiry.dto.CreateProposalRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock private ProposalRepository proposalRepository;
    @Mock private EmailNotifier emailNotifier;

    private ProposalService service() {
        return new ProposalService(proposalRepository, emailNotifier);
    }

    private CreateProposalRequest request(String website) {
        return new CreateProposalRequest(
                "김철수", "chulsoo@example.com", ProposalCategory.CORPORATE_EVENT, "제목", "본문", true, website);
    }

    @Test
    void 정상_요청이면_저장한다() {
        service().create(request(null), "1.2.3.4");

        verify(proposalRepository).save(any());
    }

    @Test
    void 허니팟이_채워져_있으면_저장하지_않는다() {
        service().create(request("https://spam.example"), "1.2.3.4");

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void 같은_IP에서_하루_세번_넘게_제출하면_거부한다() {
        ProposalService service = service();

        service.create(request(null), "1.2.3.4");
        service.create(request(null), "1.2.3.4");
        service.create(request(null), "1.2.3.4");

        assertThatThrownBy(() -> service.create(request(null), "1.2.3.4"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED));
    }

    @Test
    void 다른_IP는_서로_영향을_주지_않는다() {
        ProposalService service = service();
        service.create(request(null), "1.2.3.4");
        service.create(request(null), "1.2.3.4");
        service.create(request(null), "1.2.3.4");

        service.create(request(null), "5.6.7.8");

        verify(proposalRepository, org.mockito.Mockito.times(4)).save(any());
    }

    @Test
    void 상세_조회하면_SENT가_READ로_바뀐다() {
        Proposal proposal = new Proposal("김철수", "chulsoo@example.com", ProposalCategory.CORPORATE_EVENT, "제목", "본문");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        Proposal result = service().getForAdmin(1L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.READ);
    }

    @Test
    void 수락하면_ACCEPTED가_되고_이메일_알림을_호출한다() {
        Proposal proposal = new Proposal("김철수", "chulsoo@example.com", ProposalCategory.CORPORATE_EVENT, "제목", "본문");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        Proposal result = service().accept(1L);

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        verify(emailNotifier).notifyStatusChange(proposal);
    }

    @Test
    void 이미_처리된_제안은_다시_수락할_수_없다() {
        Proposal proposal = new Proposal("김철수", "chulsoo@example.com", ProposalCategory.CORPORATE_EVENT, "제목", "본문");
        proposal.decline();
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> service().accept(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    @Test
    void 존재하지_않는_제안은_ENTITY_NOT_FOUND를_던진다() {
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getForAdmin(99L)).isInstanceOf(BusinessException.class);
    }
}
