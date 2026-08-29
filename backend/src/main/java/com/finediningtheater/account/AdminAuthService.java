package com.finediningtheater.account;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.ratelimit.FailureLockout;
import com.finediningtheater.global.security.JwtProvider;
import com.finediningtheater.global.security.PinAuth;
import com.finediningtheater.global.security.SudoMode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 로그인·재발급과 PIN·sudo 모드를 다룬다. 회원가입 절차가 없으므로 가입 관련 로직도 없다
 * (CLAUDE.md §3.1·§3.4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final PinAuth pinAuth;
    private final SudoMode sudoMode;
    private final FailureLockout loginLockout =
            new FailureLockout(MAX_LOGIN_FAILURES, LOGIN_LOCKOUT_DURATION);

    public AdminSession login(String username, String rawPassword) {
        if (loginLockout.isLocked(username)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        AdminAccount account =
                adminAccountRepository
                        .findByUsernameAndStatus(username, AdminAccountStatus.ACTIVE)
                        .orElse(null);

        if (account == null || !passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            loginLockout.recordFailure(username);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginLockout.recordSuccess(username);
        // "PIN 5회 실패 잠금은 재로그인으로만 풀린다" — 그 재로그인이 지금 이 순간이다 (CLAUDE.md §3.4).
        pinAuth.clearLockout(account.getId());
        return issueSession(account);
    }

    /** PIN을 처음 설정하거나 바꾼다. sudo 모드가 아니라 현재 비밀번호 재확인으로 본인을 증명한다 —
     * PIN이 아직 없는 상태에서는 sudo를 요구할 수 없는 닭-달걀 문제를 피한다. */
    @Transactional
    public void setPin(Long adminId, String currentPassword, String newPin) {
        AdminAccount account =
                adminAccountRepository.findById(adminId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        account.changePin(pinAuth.hash(newPin));
    }

    /** PIN을 확인하고 통과하면 15분짜리 sudo 모드를 연다. */
    public void verifySudo(Long adminId, String pin) {
        AdminAccount account =
                adminAccountRepository.findById(adminId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        pinAuth.verify(adminId, account.getPinHash(), pin);
        sudoMode.activate(adminId);
    }

    public AdminSession refresh(String refreshToken) {
        Long adminId;
        try {
            adminId = jwtProvider.parseAdminRefreshToken(refreshToken);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount account =
                adminAccountRepository
                        .findById(adminId)
                        .filter(AdminAccount::isActive)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        return issueSession(account);
    }

    private AdminSession issueSession(AdminAccount account) {
        String accessToken =
                jwtProvider.createAdminAccessToken(account.getId(), account.getUsername(), account.getRole());
        String refreshToken = jwtProvider.createAdminRefreshToken(account.getId());
        return new AdminSession(accessToken, refreshToken, account.getUsername(), account.getRole());
    }
}
