package com.finediningtheater.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.support.SiteLocale;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @Mock private AccountRepository accountRepository;

    private final JwtProvider jwtProvider =
            new JwtProvider("test-only-secret-key-must-be-at-least-32-bytes-long-for-hs256", 15, 14);

    private Account account;

    @BeforeEach
    void setUp() throws Exception {
        account = new Account("kakao", "123", "user@example.com", "김아무개", SiteLocale.KO);
        // Account.id는 @GeneratedValue라 영속화 없이는 채울 수 없다 — refresh()가 findById(1L)로
        // 조회하는지 검증하려면 실제 id 값이 필요해서 리플렉션으로 채운다.
        Field idField = Account.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, 1L);
    }

    private MemberAuthService service() {
        return new MemberAuthService(accountRepository, jwtProvider);
    }

    @Test
    void 세션을_발급하면_access_refresh_토큰과_닉네임을_돌려준다() {
        MemberSession session = service().issueSession(account);

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(session.nickname()).isEqualTo("김아무개");
    }

    @Test
    void 유효한_refresh_토큰이면_새_세션을_발급한다() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        String refreshToken = jwtProvider.createMemberRefreshToken(1L);

        MemberSession session = service().refresh(refreshToken);

        assertThat(session.accessToken()).isNotBlank();
    }

    @Test
    void 존재하지_않는_계정의_refresh_토큰은_거부한다() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        String refreshToken = jwtProvider.createMemberRefreshToken(1L);

        assertThatThrownBy(() -> service().refresh(refreshToken)).isInstanceOf(BusinessException.class);
    }

    @Test
    void 관리자_refresh_토큰으로는_회원_세션을_재발급받을_수_없다() {
        String adminRefreshToken = jwtProvider.createAdminRefreshToken(1L);

        assertThatThrownBy(() -> service().refresh(adminRefreshToken)).isInstanceOf(BusinessException.class);
    }
}
