package com.finediningtheater.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 컨트롤러가_없는_경로는_500이_아니라_404로_내려간다() {
        NoResourceFoundException e = new NoResourceFoundException(HttpMethod.GET, "/no-such-path");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFound(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("ENTITY_NOT_FOUND");
    }

    @Test
    void 예상하지_못한_예외는_내부_정보_노출_없이_500으로_내려간다() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpectedException(new RuntimeException("secret internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().message()).doesNotContain("secret internal detail");
    }
}
