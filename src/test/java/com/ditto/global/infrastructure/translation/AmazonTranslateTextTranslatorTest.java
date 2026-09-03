package com.ditto.global.infrastructure.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.i18n.ContentLanguage;

import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

@ExtendWith(MockitoExtension.class)
class AmazonTranslateTextTranslatorTest {

    @Mock
    private TranslateClient translateClient;

    @Test
    void sendsDetectedSourceAndSelectedTargetLanguagesToAmazonTranslate() {
        TranslationProperties properties = new TranslationProperties();
        Utf8TextChunker chunker = new Utf8TextChunker(properties);
        AmazonTranslateTextTranslator translator =
                new AmazonTranslateTextTranslator(translateClient, chunker);
        when(translateClient.translateText(any(TranslateTextRequest.class)))
                .thenReturn(TranslateTextResponse.builder()
                        .translatedText("홍대 트렌드 인증 코스")
                        .build());

        String result = translator.translate(
                "弘大潮流打卡路线",
                ContentLanguage.CHINESE,
                ContentLanguage.KOREAN);

        ArgumentCaptor<TranslateTextRequest> requestCaptor =
                ArgumentCaptor.forClass(TranslateTextRequest.class);
        verify(translateClient).translateText(requestCaptor.capture());
        assertThat(requestCaptor.getValue().sourceLanguageCode()).isEqualTo("zh");
        assertThat(requestCaptor.getValue().targetLanguageCode()).isEqualTo("ko");
        assertThat(result).isEqualTo("홍대 트렌드 인증 코스");
    }
}
