package com.ditto.global.infrastructure.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class Utf8TextChunkerTest {

    @Test
    void splitsWithoutLosingCharactersOrBreakingUtf8Limit() {
        TranslationProperties properties = new TranslationProperties();
        properties.setMaxRequestBytes(12);
        Utf8TextChunker chunker = new Utf8TextChunker(properties);
        String source = "한국어 문장입니다. English sentence.";

        List<String> chunks = chunker.split(source);

        assertThat(String.join("", chunks)).isEqualTo(source);
        assertThat(chunks).allSatisfy(chunk -> assertThat(
                chunk.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(12));
    }
}
