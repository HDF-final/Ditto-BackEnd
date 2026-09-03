package com.ditto.global.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContentLanguageDetectorTest {

    @Test
    void detectsSupportedCommunityLanguages() {
        assertThat(ContentLanguageDetector.detect("더현대 서울 쇼핑 코스"))
                .isEqualTo(ContentLanguage.KOREAN);
        assertThat(ContentLanguageDetector.detect("弘大潮流打卡路线"))
                .isEqualTo(ContentLanguage.CHINESE);
        assertThat(ContentLanguageDetector.detect("推し活ショッピングコース"))
                .isEqualTo(ContentLanguage.JAPANESE);
        assertThat(ContentLanguageDetector.detect("Seoul Trend Starter!"))
                .isEqualTo(ContentLanguage.ENGLISH);
    }

    @Test
    void keepsBrandNamesFromOverridingTheSentenceLanguage() {
        assertThat(ContentLanguageDetector.detect(
                "오늘 The Hyundai Seoul에서 쇼핑했어요"))
                .isEqualTo(ContentLanguage.KOREAN);
        assertThat(ContentLanguageDetector.detect(
                "今天在 The Hyundai Seoul 逛了一条路线"))
                .isEqualTo(ContentLanguage.CHINESE);
        assertThat(ContentLanguageDetector.detect(
                "THE HYUNDAI SEOULでショッピングDAY"))
                .isEqualTo(ContentLanguage.JAPANESE);
    }

    @Test
    void returnsNullWhenThereIsNoDetectableLanguage() {
        assertThat(ContentLanguageDetector.detect("✨ 1234"))
                .isNull();
        assertThat(ContentLanguageDetector.detect("  "))
                .isNull();
    }
}
