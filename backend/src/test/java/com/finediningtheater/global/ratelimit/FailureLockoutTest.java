package com.finediningtheater.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class FailureLockoutTest {

    @Test
    void 실패_횟수가_한도_미만이면_잠기지_않는다() {
        FailureLockout lockout = new FailureLockout(5, Duration.ofMinutes(15));

        for (int i = 0; i < 4; i++) {
            lockout.recordFailure("admin");
        }

        assertThat(lockout.isLocked("admin")).isFalse();
    }

    @Test
    void 실패_횟수가_한도에_도달하면_잠긴다() {
        FailureLockout lockout = new FailureLockout(5, Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            lockout.recordFailure("admin");
        }

        assertThat(lockout.isLocked("admin")).isTrue();
    }

    @Test
    void 성공하면_실패_카운트가_초기화된다() {
        FailureLockout lockout = new FailureLockout(5, Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            lockout.recordFailure("admin");
        }
        lockout.recordSuccess("admin");

        assertThat(lockout.isLocked("admin")).isFalse();
    }

    @Test
    void 키가_다르면_서로_영향을_주지_않는다() {
        FailureLockout lockout = new FailureLockout(5, Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            lockout.recordFailure("admin");
        }

        assertThat(lockout.isLocked("someone-else")).isFalse();
    }
}
