package com.finediningtheater.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void success_응답은_data를_담고_error는_null이다() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.error()).isNull();
    }

    @Test
    void error_응답은_data가_null이고_ErrorCode를_담는다() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.ENTITY_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("ENTITY_NOT_FOUND");
    }
}
