package com.finediningtheater.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 예외는 전부 이 enum과 {@link BusinessException} 하나로 표현한다 (CLAUDE.md §7.2).
 * 도메인별 코드가 필요해지면 여기에 추가한다.
 */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값을 확인해 주세요."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", "요청한 대상을 찾을 수 없습니다."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
