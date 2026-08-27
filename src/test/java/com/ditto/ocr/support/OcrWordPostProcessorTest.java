package com.ditto.ocr.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;

class OcrWordPostProcessorTest {

    private final OcrWordPostProcessor postProcessor = new OcrWordPostProcessor();

    private RecognizedWord word(String text, double confidence,
                                double minX, double minY, double maxX, double maxY) {
        return new RecognizedWord(text, confidence, minX, minY, maxX, maxY);
    }

    @Test
    @DisplayName("층수·할인율·가격만 형태로 버리고 SALE 같은 프로모 단어는 남긴다")
    void dropsStructuralNoiseButKeepsPromoWords() {
        ClovaOcrResult cleaned = postProcessor.process(new ClovaOcrResult(List.of(
                word("SALE", 0.99, 10, 10, 400, 120),
                word("50%", 0.98, 20, 130, 220, 180),
                word("1F", 0.97, 430, 20, 480, 50),
                word("12,000원", 0.96, 40, 300, 180, 340),
                word("TAMBURINS", 0.92, 40, 200, 360, 280))));

        assertThat(cleaned.getWords()).extracting(RecognizedWord::getText)
                .contains("SALE", "TAMBURINS")
                .doesNotContain("1F", "50%", "12,000원");
    }

    @Test
    @DisplayName("같은 줄에서 맞닿은 POP 와 MART 를 한 상호로 붙인다")
    void mergesSplitBrandOnSameLine() {
        ClovaOcrResult cleaned = postProcessor.process(new ClovaOcrResult(List.of(
                word("POP", 0.94, 10, 50, 80, 90),
                word("MART", 0.93, 88, 52, 180, 92))));

        assertThat(cleaned.getWords()).hasSize(1);
        assertThat(cleaned.primaryText()).isEqualTo("POP MART");
    }

    @Test
    @DisplayName("다른 줄의 단어는 붙이지 않는다")
    void doesNotMergeWordsOnDifferentLines() {
        ClovaOcrResult cleaned = postProcessor.process(new ClovaOcrResult(List.of(
                word("POP", 0.94, 10, 10, 80, 40),
                word("MART", 0.93, 10, 80, 80, 110))));

        assertThat(cleaned.getWords()).extracting(RecognizedWord::getText)
                .containsExactlyInAnyOrder("POP", "MART");
    }

    @Test
    @DisplayName("SALE·OPEN 만 있어도 후처리에서 비우지 않는다. 매칭이 카탈로그로 걸러낸다")
    void promoWordsSurvivePostProcess() {
        ClovaOcrResult cleaned = postProcessor.process(new ClovaOcrResult(List.of(
                word("SALE", 0.99, 10, 10, 400, 120),
                word("OPEN", 0.98, 20, 140, 200, 180))));

        assertThat(cleaned.getWords()).extracting(RecognizedWord::getText)
                .containsExactlyInAnyOrder("SALE", "OPEN");
    }

    @Test
    @DisplayName("bbox 가 없으면 분리 단어는 그대로 두고 층·가격만 버린다")
    void withoutBboxOnlyFiltersStructuralNoise() {
        ClovaOcrResult cleaned = postProcessor.process(new ClovaOcrResult(List.of(
                new RecognizedWord("SALE", 0.99, 5000),
                new RecognizedWord("1F", 0.97, 400),
                new RecognizedWord("POP", 0.94, 2000),
                new RecognizedWord("MART", 0.93, 1800))));

        assertThat(cleaned.getWords()).extracting(RecognizedWord::getText)
                .contains("SALE", "POP", "MART")
                .doesNotContain("1F");
    }
}
