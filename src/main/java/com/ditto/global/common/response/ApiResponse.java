package com.ditto.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 모든 API 응답의 공통 포맷.
 *
 * <pre>
 * {
 *   "success": true,
 *   "code": "SUCCESS",
 *   "message": "요청이 정상 처리되었습니다.",
 *   "data": { ... }
 * }
 * </pre>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청이 정상 처리되었습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청이 정상 처리되었습니다.")
                .build();
    }
}
