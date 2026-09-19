package com.finediningtheater.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.SiteLocale;
import com.finediningtheater.review.ReviewService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private ReviewService reviewService;

    private MemberWithdrawalService service() {
        return new MemberWithdrawalService(accountRepository, reviewService);
    }

    private Account account() {
        return new Account("kakao", "kakao-123", "a@example.com", "김아무개", SiteLocale.KO);
    }

    @Test
    void 탈퇴하면_개인정보를_지우고_상태를_바꾸며_글의_이름_연락처를_지운다() {
        Account account = account();
        when(accountRepository.findById(4L)).thenReturn(Optional.of(account));

        service().withdraw(4L);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
        assertThat(account.getNickname()).isEqualTo("탈퇴한 회원");
        assertThat(account.getEmail()).isNull();
        assertThat(account.getProviderUserId()).isNull();
        verify(reviewService).clearAuthorInfo(4L);
    }

    @Test
    void 이미_탈퇴한_계정은_다시_탈퇴할_수_없다() {
        Account account = account();
        account.withdraw();
        when(accountRepository.findById(4L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service().withdraw(4L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        verify(reviewService, never()).clearAuthorInfo(4L);
    }

    @Test
    void 탈퇴한_계정은_refresh할_수_없다() {
        Account account = account();
        account.withdraw();

        assertThat(account.isActive()).isFalse();
    }
}
