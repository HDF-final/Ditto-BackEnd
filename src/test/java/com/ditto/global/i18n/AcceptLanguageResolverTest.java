package com.ditto.global.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AcceptLanguageResolverTest {

    @Test
    void resolvesSupportedLanguageWithRegionAndQuality() {
        assertThat(AcceptLanguageResolver.resolve("ja-JP,en;q=0.8"))
                .isEqualTo(ContentLanguage.JAPANESE);
        assertThat(AcceptLanguageResolver.resolve("zh-CN,zh;q=0.9"))
                .isEqualTo(ContentLanguage.CHINESE);
        assertThat(AcceptLanguageResolver.resolve("en-US"))
                .isEqualTo(ContentLanguage.ENGLISH);
    }

    @Test
    void fallsBackToKoreanForMissingInvalidOrUnsupportedHeader() {
        assertThat(AcceptLanguageResolver.resolve(null)).isEqualTo(ContentLanguage.KOREAN);
        assertThat(AcceptLanguageResolver.resolve("")).isEqualTo(ContentLanguage.KOREAN);
        assertThat(AcceptLanguageResolver.resolve("not a language header"))
                .isEqualTo(ContentLanguage.KOREAN);
        assertThat(AcceptLanguageResolver.resolve("fr-FR"))
                .isEqualTo(ContentLanguage.KOREAN);
    }
}
