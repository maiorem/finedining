package com.finediningtheater.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.finediningtheater.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    /**
     * @PreAuthorize 거부는 500이 아니라 403으로 내려가야 한다 — GET permitAll 와일드카드 아래
     * 놓인 /manage 관리자 엔드포인트를 익명으로 두드렸을 때 이걸 놓치면 500이 새어나간다
     * (2026-08-27 발견, Artist/Casting 작업 중).
     */
    @Test
    void PreAuthorize_거부는_500이_아니라_403으로_내려간다() {
        AuthorizationDeniedException e =
                new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false));

        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthorizationDenied(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
    }
}
