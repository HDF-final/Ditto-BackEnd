package com.ditto.ocr.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OcrStopwordsTest {

    @Test
    @DisplayName("층·할인율·가격·시각만 구조적 노이즈다")
    void structuralShapesAreNoise() {
        assertThat(OcrStopwords.isStructuralNoise("1F")).isTrue();
        assertThat(OcrStopwords.isStructuralNoise("B2")).isTrue();
        assertThat(OcrStopwords.isStructuralNoise("50%")).isTrue();
        assertThat(OcrStopwords.isStructuralNoise("12,000원")).isTrue();
        assertThat(OcrStopwords.isStructuralNoise("10:30")).isTrue();
    }

    @Test
    @DisplayName("세일중·SALE 은 사전으로 지우지 않는다")
    void promoCopyIsNotAWordList() {
        assertThat(OcrStopwords.isStructuralNoise("세일중")).isFalse();
        assertThat(OcrStopwords.isStructuralNoise("SALE")).isFalse();
        assertThat(OcrStopwords.isStructuralNoise("OPEN")).isFalse();
        assertThat(OcrStopwords.isStructuralNoise("TAMBURINS")).isFalse();
        assertThat(OcrStopwords.isStructuralNoise("1985")).isFalse();
    }
}
