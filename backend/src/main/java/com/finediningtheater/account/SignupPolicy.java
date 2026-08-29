package com.finediningtheater.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 가입 정책은 코드가 아니라 설정이다(CLAUDE.md §3.2). 이 분기는 이 클래스 한 곳에만 존재한다 —
 * {@link AccountService}가 여기에만 물어본다. 지금은 OPEN을 쓰지만, 리뷰 스팸이 터지면
 * INVITE_ONLY로 즉시 잠글 수 있는 유일한 스위치다.
 */
@Component
public class SignupPolicy {

    public enum Policy {
        OPEN,
        INVITE_ONLY
    }

    private final SignupAllowlistRepository signupAllowlistRepository;
    private final Policy policy;

    public SignupPolicy(
            SignupAllowlistRepository signupAllowlistRepository,
            @Value("${app.signup.policy:OPEN}") Policy policy) {
        this.signupAllowlistRepository = signupAllowlistRepository;
        this.policy = policy;
    }

    public boolean canSignUp(String provider, String providerUserId, String email) {
        if (policy == Policy.OPEN) {
            return true;
        }
        if (signupAllowlistRepository.existsByProviderAndProviderUserId(provider, providerUserId)) {
            return true;
        }
        return email != null && signupAllowlistRepository.existsByEmail(email);
    }
}
