package com.ditto.global.exception;

import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * 실패 응답 본문 DTO. ApiResponse 의 실패 표현과 동일한 최상위 필드를 공유한다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> errors;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message)
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(FieldErrorDetail.from(bindingResult))
                .build();
    }

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private final String field;
        private final String value;
        private final String reason;

        private static List<FieldErrorDetail> from(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(FieldErrorDetail::from)
                    .toList();
        }

        private static FieldErrorDetail from(FieldError fieldError) {
            return FieldErrorDetail.builder()
                    .field(fieldError.getField())
                    .value(fieldError.getRejectedValue() == null ? "" : fieldError.getRejectedValue().toString())
                    .reason(fieldError.getDefaultMessage())
                    .build();
        }
    }
}
