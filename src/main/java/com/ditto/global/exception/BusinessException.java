package com.ditto.global.exception;

import lombok.Getter;

/**
 * 서비스 로직에서 발생하는 모든 예외의 최상위 타입.
 * 도메인별 예외는 이 클래스를 상속해 세분화한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
