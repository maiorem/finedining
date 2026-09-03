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
    DUPLICATE_SLUG(HttpStatus.CONFLICT, "DUPLICATE_SLUG", "이미 사용 중인 슬러그입니다."),
    INVALID_STATE_TRANSITION(
            HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "지금 상태에서는 처리할 수 없습니다."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    POST_NOT_OWNED(
            HttpStatus.FORBIDDEN, "POST_NOT_OWNED", "본인이 작성한 글만 수정·삭제할 수 있습니다."),
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(
            HttpStatus.LOCKED,
            "ACCOUNT_LOCKED",
            "로그인 시도가 너무 많아 잠겼습니다. 잠시 후 다시 시도해 주세요."),
    PIN_REQUIRED(HttpStatus.FORBIDDEN, "PIN_REQUIRED", "민감한 동작을 수행하려면 먼저 PIN을 확인해 주세요."),
    PIN_INVALID(HttpStatus.UNAUTHORIZED, "PIN_INVALID", "PIN이 올바르지 않습니다."),
    PIN_LOCKED(
            HttpStatus.LOCKED,
            "PIN_LOCKED",
            "PIN 입력이 너무 많이 틀려 잠겼습니다. 다시 로그인해 주세요."),
    WEAK_PIN(
            HttpStatus.BAD_REQUEST,
            "WEAK_PIN",
            "너무 단순한 번호입니다. 000000·123456이나 연속·동일 숫자는 피해 주세요."),
    SIGNUP_NOT_ALLOWED(
            HttpStatus.FORBIDDEN, "SIGNUP_NOT_ALLOWED", "지금은 초대받은 사용자만 가입할 수 있습니다."),
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
