package com.finediningtheater.account;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import com.finediningtheater.global.support.SiteLocale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카카오 OAuth2 로그인 성공 후 계정을 찾거나 만든다(CLAUDE.md §7.4). SuccessHandler가
 * OAuth2User 속성을 여기 넘기고, 가입을 받아들일지는 {@link SignupPolicy}에게만 물어본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final SignupPolicy signupPolicy;

    @Transactional
    public Account findOrCreate(String provider, String providerUserId, String email, String nickname) {
        return accountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> create(provider, providerUserId, email, nickname));
    }

    private Account create(String provider, String providerUserId, String email, String nickname) {
        if (!signupPolicy.canSignUp(provider, providerUserId, email)) {
            throw new BusinessException(ErrorCode.SIGNUP_NOT_ALLOWED);
        }
        return accountRepository.save(new Account(provider, providerUserId, email, nickname, SiteLocale.KO));
    }
}
