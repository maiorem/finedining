package com.finediningtheater.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PinAuthTest {

    private final PinAuth pinAuth = new PinAuth(new BCryptPasswordEncoder());

    @Test
    void 정상적인_PIN은_해시하고_그대로_검증할_수_있다() {
        String hash = pinAuth.hash("482913");

        pinAuth.verify(1L, hash, "482913");
    }

    @Test
    void 전부_0인_PIN은_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.WEAK_PIN));
    }

    @Test
    void 오름차순_연속_숫자는_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("123456")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 내림차순_연속_숫자도_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("654321")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 동일한_숫자_반복도_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("777777")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 길이가_6자리가_아니면_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("12345")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> pinAuth.hash("1234567")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 숫자가_아니면_거부한다() {
        assertThatThrownBy(() -> pinAuth.hash("abcdef")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 평범한_PIN은_허용한다() {
        String hash = pinAuth.hash("482913");
        assertThat(hash).isNotBlank();
    }

    @Test
    void 틀린_PIN_다섯번이면_잠기고_맞는_PIN도_거부한다() {
        String hash = pinAuth.hash("482913");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> pinAuth.verify(2L, hash, "000000"))
                    .isInstanceOf(BusinessException.class);
        }

        assertThatThrownBy(() -> pinAuth.verify(2L, hash, "482913"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PIN_LOCKED));
    }

    @Test
    void clearLockout_이후에는_다시_검증할_수_있다() {
        String hash = pinAuth.hash("482913");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> pinAuth.verify(3L, hash, "000000"))
                    .isInstanceOf(BusinessException.class);
        }

        pinAuth.clearLockout(3L);

        pinAuth.verify(3L, hash, "482913");
    }

    @Test
    void PIN이_아직_설정되지_않았으면_거부한다() {
        assertThatThrownBy(() -> pinAuth.verify(4L, null, "482913"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PIN_INVALID));
    }
}
