package com.ditto.ocr.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

/**
 * OCR 입력 이미지 사전 검증. 검증은 다른 의존성보다 먼저 실행되므로,
 * 나머지 협력 객체는 null 로 두고 차단 케이스만 확인한다.
 */
class OcrNavigationServiceTest {

    private final OcrNavigationService service =
            new OcrNavigationService(null, null, null, null, null, null, null, null);

    @Test
    @DisplayName("빈 이미지는 INVALID_IMAGE_FILE 로 사전 차단된다")
    void rejectsEmptyImage() {
        MockMultipartFile empty = new MockMultipartFile("image", "sign.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.recognizeLocation(empty))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    @DisplayName("10MB 초과 이미지는 IMAGE_SIZE_EXCEEDED 로 사전 차단된다")
    void rejectsOversizedImage() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile oversized = new MockMultipartFile("image", "sign.jpg", "image/jpeg", big);

        assertThatThrownBy(() -> service.recognizeLocation(oversized))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("지원하지 않는 형식(PDF)은 UNSUPPORTED_IMAGE_TYPE 로 사전 차단된다")
    void rejectsUnsupportedType() {
        MockMultipartFile pdf = new MockMultipartFile("image", "sign.pdf", "application/pdf", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.recognizeLocation(pdf))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("content-type 이 없으면 지원 형식으로 인정하지 않는다")
    void rejectsMissingContentType() {
        MockMultipartFile noType = new MockMultipartFile("image", "sign.jpg", null, new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.recognizeLocation(noType))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("핸드폰 JPEG 사진은 사전 검증을 통과한다(이후 협력 객체에서 멈춤)")
    void acceptsPhoneJpeg() {
        MockMultipartFile jpeg = new MockMultipartFile("image", "sign.jpg", "image/jpeg", new byte[] {1, 2, 3});

        // 검증을 통과하면 BusinessException 이 아니라 이후 단계(null 의존성)에서 멈춘다.
        assertThatThrownBy(() -> service.recognizeLocation(jpeg))
                .isNotInstanceOf(BusinessException.class);
    }
}
