package com.ditto.ocr.support;

import lombok.Getter;

/**
 * CLOVA 로 보낼 이미지. 전처리를 건너뛰었으면 {@code transformed=false} 이고 원본 바이트다.
 */
@Getter
public class PreprocessedImage {

    private final byte[] bytes;
    private final String format;
    private final boolean transformed;
    private final int originalBytes;
    private final int originalWidth;
    private final int originalHeight;

    public PreprocessedImage(byte[] bytes, String format, boolean transformed,
                             int originalBytes, int originalWidth, int originalHeight) {
        this.bytes = bytes;
        this.format = format;
        this.transformed = transformed;
        this.originalBytes = originalBytes;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
    }

    public static PreprocessedImage passthrough(byte[] original, String format) {
        return new PreprocessedImage(original, format, false,
                original == null ? 0 : original.length, 0, 0);
    }
}
