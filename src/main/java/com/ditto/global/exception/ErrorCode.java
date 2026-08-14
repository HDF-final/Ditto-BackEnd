package com.ditto.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 전역 에러 코드.
 * HTTP 상태 + 비즈니스 코드 + 기본 메시지를 한 곳에서 관리한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== Common =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "요청 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C006", "필수 요청 파라미터가 누락되었습니다."),

    // ===== Auth / Security =====
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "세션이 만료되었습니다. 다시 로그인해 주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A005", "이메일 또는 비밀번호가 올바르지 않습니다."),
    LOGIN_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "L001", "인증되지 않은 사용자입니다."),
    LOGIN_PASSWORD_REQUIRED(HttpStatus.UNAUTHORIZED, "L002", "비밀번호를 입력해주세요."),

    // ===== Signup =====
    DUPLICATE_SIGNUP_EMAIL(HttpStatus.BAD_REQUEST, "S001", "이미 존재하는 이메일입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "S002", "이메일 형식이 올바르지 않습니다."),
    DEFAULT_COUNTRY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "S005", "기본 국가 정보를 찾을 수 없습니다."),

    // ===== User =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),

    // ===== Course =====
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "코스를 찾을 수 없습니다."),
    NOT_COURSE_OWNER(HttpStatus.FORBIDDEN, "CR002", "코스에 대한 권한이 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR003", "장소를 찾을 수 없습니다."),
    DUPLICATE_PLACE_IN_COURSE(HttpStatus.BAD_REQUEST, "CR004", "코스에 같은 장소가 중복되어 있습니다."),
    COURSE_NOT_PUBLIC(HttpStatus.FORBIDDEN, "CR005", "공개된 코스만 복사할 수 있습니다."),

    // ===== Community =====
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "댓글을 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "CM003", "이미 좋아요한 코스입니다."),

    // ===== Navigation / Mobile =====
    MAP_MANIFEST_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "지도 매니페스트를 찾을 수 없습니다."),
    INVALID_ACCESS_CODE(HttpStatus.BAD_REQUEST, "N002", "유효하지 않은 접속 코드입니다."),

    // ===== Storage / S3 =====
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "ST001", "업로드할 이미지가 올바르지 않습니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "ST002", "이미지 크기는 10MB를 초과할 수 없습니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ST003", "지원하지 않는 이미지 형식입니다."),
    INVALID_STORAGE_PATH(HttpStatus.BAD_REQUEST, "ST004", "올바르지 않은 이미지 저장 경로입니다."),
    S3_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "ST005", "이미지 업로드에 실패했습니다."),
    S3_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "ST006", "이미지 삭제에 실패했습니다."),
    S3_URL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "ST007", "이미지 조회 URL 생성에 실패했습니다."),

    // ===== External (AI / OCR) =====
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "E001", "AI 서비스 처리 중 오류가 발생했습니다."),
    OCR_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "E002", "OCR 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
