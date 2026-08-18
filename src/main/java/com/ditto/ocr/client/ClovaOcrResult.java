package com.ditto.ocr.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CLOVA OCR 인식 결과를 서비스가 쓰기 좋게 추린 값.
 *
 * <p>간판에서 가장 큰 글자(면적 최대 필드)를 브랜드명으로 본다.
 */
@Getter
@AllArgsConstructor
public class ClovaOcrResult {

    /** 브랜드명으로 채택한 텍스트. 인식된 글자가 하나도 없으면 {@code null}. */
    private final String brandName;

    /** 채택한 텍스트의 OCR 신뢰도(0~1). 없으면 {@code 0}. */
    private final double confidence;

    public boolean isEmpty() {
        return brandName == null || brandName.isBlank();
    }

    public static ClovaOcrResult empty() {
        return new ClovaOcrResult(null, 0.0);
    }
}
