package com.finediningtheater.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finediningtheater.global.error.BusinessException;
import com.finediningtheater.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

class BookingUrlValidatorTest {

    private final BookingUrlValidator validator = new BookingUrlValidator("booking.naver.com,m.booking.naver.com");

    @Test
    void 허용된_호스트는_통과한다() {
        validator.validate("https://booking.naver.com/bizes/1/items/1");
        validator.validate("https://m.booking.naver.com/bizes/1/items/1");
    }

    @Test
    void 허용되지_않은_호스트는_거부한다() {
        assertThatThrownBy(() -> validator.validate("https://evil.example.com/booking"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void 잘못된_형식의_URL은_거부한다() {
        assertThatThrownBy(() -> validator.validate("not a url"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
