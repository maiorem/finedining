package com.finediningtheater.global.response;

import com.finediningtheater.global.error.ErrorCode;

/**
 * 모든 컨트롤러 응답을 감싸는 봉투 (CLAUDE.md §7.2).
 * 성공 시 error는 null, 실패 시 data는 null이다.
 */
public record ApiResponse<T>(boolean success, T data, Error error) {

    public record Error(String code, String message) {
        public static Error from(ErrorCode errorCode) {
            return new Error(errorCode.getCode(), errorCode.getMessage());
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, Error.from(errorCode));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new Error(errorCode.getCode(), message));
    }
}
