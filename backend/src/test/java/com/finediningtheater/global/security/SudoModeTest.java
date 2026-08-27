package com.finediningtheater.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

class SudoModeTest {

    private final SudoMode sudoMode = new SudoMode();

    @Test
    void 활성화_전에는_비활성이다() {
        assertThat(sudoMode.isActive(1L)).isFalse();
    }

    @Test
    void 활성화하면_isActive가_참이다() {
        sudoMode.activate(1L);

        assertThat(sudoMode.isActive(1L)).isTrue();
    }

    @Test
    void 관리자별로_독립적으로_동작한다() {
        sudoMode.activate(1L);

        assertThat(sudoMode.isActive(2L)).isFalse();
    }

    @Test
    void requireActive는_비활성이면_PIN_REQUIRED를_던진다() {
        assertThatThrownBy(() -> sudoMode.requireActive(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PIN_REQUIRED));
    }

    @Test
    void requireActive는_활성이면_통과한다() {
        sudoMode.activate(1L);

        sudoMode.requireActive(1L);
    }
}
