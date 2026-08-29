package com.finediningtheater.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.SiteLocale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private SignupPolicy signupPolicy;

    private AccountService service() {
        return new AccountService(accountRepository, signupPolicy);
    }

    @Test
    void 이미_있는_계정이면_새로_만들지_않고_그대로_반환한다() {
        Account existing = new Account("kakao", "123", "user@example.com", "김아무개", SiteLocale.KO);
        when(accountRepository.findByProviderAndProviderUserId("kakao", "123")).thenReturn(Optional.of(existing));

        Account result = service().findOrCreate("kakao", "123", "user@example.com", "김아무개");

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void 처음_로그인이면_가입정책을_확인하고_새_계정을_만든다() {
        when(accountRepository.findByProviderAndProviderUserId("kakao", "999")).thenReturn(Optional.empty());
        when(signupPolicy.canSignUp("kakao", "999", null)).thenReturn(true);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = service().findOrCreate("kakao", "999", null, "새회원");

        assertThat(result.getNickname()).isEqualTo("새회원");
        assertThat(result.getEmail()).isNull();
    }

    @Test
    void 가입정책이_거부하면_SIGNUP_NOT_ALLOWED를_던진다() {
        when(accountRepository.findByProviderAndProviderUserId("kakao", "999")).thenReturn(Optional.empty());
        when(signupPolicy.canSignUp("kakao", "999", null)).thenReturn(false);

        assertThatThrownBy(() -> service().findOrCreate("kakao", "999", null, "새회원"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.SIGNUP_NOT_ALLOWED));
        verify(accountRepository, never()).save(any());
    }
}
