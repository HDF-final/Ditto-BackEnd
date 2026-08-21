package com.ditto.ocr.client;

import java.util.List;

import lombok.Getter;

/**
 * CLOVA OCR 인식 결과를 서비스가 쓰기 좋게 추린 값.
 *
 * <p>인식된 텍스트 조각들을 간판에서의 도드라짐(면적) 내림차순으로 담는다.
 * 가장 도드라진 조각을 대표 브랜드명으로 보되, 매칭은 상위 여러 조각을 후보로 함께 쓴다.
 */
@Getter
public class ClovaOcrResult {

    /** 도드라짐(면적) 내림차순으로 정렬된 인식 텍스트 조각들. */
    private final List<RecognizedWord> words;

    public ClovaOcrResult(List<RecognizedWord> words) {
        this.words = words == null ? List.of() : List.copyOf(words);
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    public static ClovaOcrResult empty() {
        return new ClovaOcrResult(List.of());
    }

    /** 대표 브랜드명(가장 도드라진 조각). 인식된 글자가 없으면 {@code null}. */
    public String primaryText() {
        return isEmpty() ? null : words.get(0).getText();
    }

    /** 대표 조각의 OCR 신뢰도(0~1). 없으면 {@code 0}. */
    public double primaryConfidence() {
        return isEmpty() ? 0.0 : words.get(0).getConfidence();
    }
}
