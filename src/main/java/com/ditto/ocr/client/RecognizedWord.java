package com.ditto.ocr.client;

import lombok.Getter;

/**
 * CLOVA OCR 이 인식한 텍스트 조각 하나.
 *
 * <p>간판 매칭은 "가장 큰 글자 하나"만 보지 않고, 인식된 여러 조각을 후보로 다룬다.
 * {@code area}(바운딩 박스 면적)는 간판에서의 도드라짐 정도, {@code confidence}는 OCR 신뢰도다.
 * min/max 좌표는 같은 줄의 분리된 단어(POP + MART)를 붙일 때 쓴다.
 */
@Getter
public class RecognizedWord {

    private final String text;
    private final double confidence;
    private final double area;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public RecognizedWord(String text, double confidence, double area) {
        this(text, confidence, area, 0, 0, 0, 0);
    }

    public RecognizedWord(String text, double confidence,
                          double minX, double minY, double maxX, double maxY) {
        this(text, confidence, Math.max(0, maxX - minX) * Math.max(0, maxY - minY),
                minX, minY, maxX, maxY);
    }

    public RecognizedWord(String text, double confidence, double area,
                          double minX, double minY, double maxX, double maxY) {
        this.text = text;
        this.confidence = confidence;
        this.area = area;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean hasBoundingBox() {
        return maxX > minX && maxY > minY;
    }

    public double width() {
        return Math.max(0, maxX - minX);
    }

    public double height() {
        return Math.max(0, maxY - minY);
    }

    public double centerX() {
        return (minX + maxX) / 2.0;
    }

    public double centerY() {
        return (minY + maxY) / 2.0;
    }
}
