package com.ditto.ocr.client.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * CLOVA OCR General API 요청의 {@code message} 파트.
 *
 * <p>멀티파트 전송이므로 이미지 바이너리는 {@code file} 파트로 따로 보내고,
 * 여기 {@code images} 에는 {@code data} 없이 메타(format·name)만 담는다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ClovaOcrRequest {

    private final String version;
    private final String requestId;
    private final long timestamp;
    private final List<Image> images;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Image {
        private final String format;
        private final String name;
    }
}
