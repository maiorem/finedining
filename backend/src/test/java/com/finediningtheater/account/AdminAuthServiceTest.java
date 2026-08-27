package com.finediningtheater.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.PinAuth;
import com.finediningtheater.global.security.SudoMode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private AdminAccountRepository adminAccountRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider =
            new JwtProvider("test-only-secret-key-must-be-at-least-32-bytes-long-for-hs256", 15, 14);
    private final PinAuth pinAuth = new PinAuth(passwordEncoder);
    private final SudoMode sudoMode = new SudoMode();

    private AdminAccount account;

    @BeforeEach
    void setUp() throws Exception {
        account = new AdminAccount("admin", passwordEncoder.encode("correct-password"), AdminRole.EDITOR);
        // AdminAccount.id는 @GeneratedValue라 영속화 없이는 채울 수 없다 — PIN 잠금 해제가
        // account.getId()로 키를 맞추는지 검증하려면(로그인 성공 시 clearLockout) 실제 id 값이
        // 필요해서 리플렉션으로 채운다.
        Field idField = AdminAccount.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, 1L);
    }

    private AdminAuthService service() {
        return new AdminAuthService(adminAccountRepository, passwordEncoder, jwtProvider, pinAuth, sudoMode);
    }

    @Test
    void 아이디와_비밀번호가_맞으면_세션을_발급한다() {
        when(adminAccountRepository.findByUsernameAndStatus("admin", AdminAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));

        AdminSession session = service().login("admin", "correct-password");

        assertThat(session.username()).isEqualTo("admin");
        assertThat(session.role()).isEqualTo(AdminRole.EDITOR);
        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshToken()).isNotBlank();
    }

    @Test
    void 비밀번호가_틀리면_INVALID_CREDENTIALS를_던진다() {
        when(adminAccountRepository.findByUsernameAndStatus("admin", AdminAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service().login("admin", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void 존재하지_않는_아이디도_같은_INVALID_CREDENTIALS를_던진다() {
        when(adminAccountRepository.findByUsernameAndStatus("nobody", AdminAccountStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().login("nobody", "anything"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void 다섯번_틀리면_여섯번째는_비밀번호가_맞아도_잠긴다() {
        when(adminAccountRepository.findByUsernameAndStatus("admin", AdminAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        AdminAuthService service = service();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login("admin", "wrong-password"))
                    .isInstanceOf(BusinessException.class);
        }

        assertThatThrownBy(() -> service.login("admin", "correct-password"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    void 유효한_refresh_토큰이면_새_세션을_발급한다() {
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        String refreshToken = jwtProvider.createRefreshToken(1L);

        AdminSession session = service().refresh(refreshToken);

        assertThat(session.accessToken()).isNotBlank();
    }

    @Test
    void 비활성_계정의_refresh_토큰은_거부한다() {
        account.disable();
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        String refreshToken = jwtProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> service().refresh(refreshToken)).isInstanceOf(BusinessException.class);
    }

    @Test
    void access_토큰을_refresh_엔드포인트에_쓰면_거부한다() {
        String accessToken = jwtProvider.createAccessToken(1L, "admin", AdminRole.EDITOR);

        assertThatThrownBy(() -> service().refresh(accessToken)).isInstanceOf(BusinessException.class);
    }

    @Test
    void 현재_비밀번호가_맞으면_PIN을_설정한다() {
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        service().setPin(1L, "correct-password", "482913");

        assertThat(passwordEncoder.matches("482913", account.getPinHash())).isTrue();
    }

    @Test
    void 현재_비밀번호가_틀리면_PIN을_설정하지_않는다() {
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service().setPin(1L, "wrong-password", "482913"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        assertThat(account.getPinHash()).isNull();
    }

    @Test
    void 연속된_숫자_PIN은_거부한다() {
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service().setPin(1L, "correct-password", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.WEAK_PIN));
    }

    @Test
    void PIN이_맞으면_sudo_모드가_열린다() {
        account.changePin(passwordEncoder.encode("482913"));
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        service().verifySudo(1L, "482913");

        assertThat(sudoMode.isActive(1L)).isTrue();
    }

    @Test
    void PIN이_틀리면_sudo_모드가_열리지_않는다() {
        account.changePin(passwordEncoder.encode("482913"));
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service().verifySudo(1L, "000001")).isInstanceOf(BusinessException.class);
        assertThat(sudoMode.isActive(1L)).isFalse();
    }

    @Test
    void PIN_다섯번_실패하면_재로그인_전까지는_맞는_PIN도_거부한다() {
        account.changePin(passwordEncoder.encode("482913"));
        when(adminAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(adminAccountRepository.findByUsernameAndStatus("admin", AdminAccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        AdminAuthService service = service();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verifySudo(1L, "000001")).isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> service.verifySudo(1L, "482913"))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PIN_LOCKED));

        // 재로그인하면 풀린다 (CLAUDE.md §3.4)
        service.login("admin", "correct-password");
        service.verifySudo(1L, "482913");
        assertThat(sudoMode.isActive(1L)).isTrue();
    }
}
