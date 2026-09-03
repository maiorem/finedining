package com.finediningtheater.account;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 회원 세션 재발급. 로그인 자체는 카카오 OAuth2 흐름(OAuth2LoginSuccessHandler)이 맡고,
 * 여기서는 그 이후 재발급·세션 모양만 관리한다(CLAUDE.md §7.4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final AccountRepository accountRepository;
    private final JwtProvider jwtProvider;

    public MemberSession issueSession(Account account) {
        String accessToken = jwtProvider.createMemberAccessToken(account.getId(), account.getNickname());
        String refreshToken = jwtProvider.createMemberRefreshToken(account.getId());
        return new MemberSession(account.getId(), accessToken, refreshToken, account.getNickname());
    }

    public MemberSession refresh(String refreshToken) {
        Long accountId;
        try {
            accountId = jwtProvider.parseMemberRefreshToken(refreshToken);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Account account =
                accountRepository
                        .findById(accountId)
                        .filter(Account::isActive)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        return issueSession(account);
    }
}
