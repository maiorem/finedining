package com.finediningtheater.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignupPolicyTest {

    @Mock private SignupAllowlistRepository signupAllowlistRepository;

    @Test
    void OPEN_정책은_허용목록을_보지_않고_전부_허용한다() {
        SignupPolicy policy = new SignupPolicy(signupAllowlistRepository, SignupPolicy.Policy.OPEN);

        assertThat(policy.canSignUp("kakao", "999", "nobody@example.com")).isTrue();
    }

    @Test
    void INVITE_ONLY_정책은_provider_userId가_허용목록에_있으면_허용한다() {
        SignupPolicy policy = new SignupPolicy(signupAllowlistRepository, SignupPolicy.Policy.INVITE_ONLY);
        when(signupAllowlistRepository.existsByProviderAndProviderUserId("kakao", "123")).thenReturn(true);

        assertThat(policy.canSignUp("kakao", "123", null)).isTrue();
    }

    @Test
    void INVITE_ONLY_정책은_email이_허용목록에_있으면_허용한다() {
        SignupPolicy policy = new SignupPolicy(signupAllowlistRepository, SignupPolicy.Policy.INVITE_ONLY);
        when(signupAllowlistRepository.existsByProviderAndProviderUserId("kakao", "999")).thenReturn(false);
        when(signupAllowlistRepository.existsByEmail("allowed@example.com")).thenReturn(true);

        assertThat(policy.canSignUp("kakao", "999", "allowed@example.com")).isTrue();
    }

    @Test
    void INVITE_ONLY_정책은_허용목록에_없으면_거부한다() {
        SignupPolicy policy = new SignupPolicy(signupAllowlistRepository, SignupPolicy.Policy.INVITE_ONLY);
        when(signupAllowlistRepository.existsByProviderAndProviderUserId("kakao", "999")).thenReturn(false);

        assertThat(policy.canSignUp("kakao", "999", null)).isFalse();
    }
}
