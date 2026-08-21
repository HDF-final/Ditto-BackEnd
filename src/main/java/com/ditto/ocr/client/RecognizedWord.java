package com.ditto.ocr.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CLOVA OCR 이 인식한 텍스트 조각 하나.
 *
 * <p>간판 매칭은 "가장 큰 글자 하나"만 보지 않고, 인식된 여러 조각을 후보로 다룬다.
 * {@code area}(바운딩 박스 면적)는 간판에서의 도드라짐 정도, {@code confidence}는 OCR 신뢰도다.
 */
@Getter
@AllArgsConstructor
public class RecognizedWord {

    private final String text;
    private final double confidence;
    private final double area;
}
