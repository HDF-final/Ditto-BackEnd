package com.ditto.global.infrastructure.translation;

import org.springframework.stereotype.Component;

import com.ditto.global.i18n.ContentLanguage;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;

@Component
@RequiredArgsConstructor
public class AmazonTranslateTextTranslator implements TextTranslator {

    private final TranslateClient translateClient;
    private final Utf8TextChunker textChunker;

    @Override
    public String translate(String sourceText, ContentLanguage targetLanguage) {
        StringBuilder translated = new StringBuilder();
        for (String chunk : textChunker.split(sourceText)) {
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .sourceLanguageCode(ContentLanguage.KOREAN.getCode())
                    .targetLanguageCode(targetLanguage.getCode())
                    .text(chunk)
                    .build();
            String translatedChunk = translateClient.translateText(request).translatedText();
            if (translatedChunk == null || translatedChunk.isBlank()) {
                throw new IllegalStateException("Amazon Translate returned an empty result");
            }
            translated.append(translatedChunk);
        }
        return translated.toString();
    }
}
