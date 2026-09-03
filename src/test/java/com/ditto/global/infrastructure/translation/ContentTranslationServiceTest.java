package com.ditto.global.infrastructure.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.repository.TranslationCacheEntry;
import com.ditto.global.infrastructure.translation.repository.TranslationCacheMapper;

@ExtendWith(MockitoExtension.class)
class ContentTranslationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T06:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private TranslationCacheMapper translationCacheMapper;

    @Mock
    private TextTranslator textTranslator;

    private TranslationProperties properties;
    private ContentTranslationService service;

    @BeforeEach
    void setUp() {
        properties = new TranslationProperties();
        properties.setEnabled(true);
        service = new ContentTranslationService(
                translationCacheMapper, textTranslator, properties, CLOCK);
    }

    @Test
    void returnsCachedTranslationWithoutCallingAws() {
        TranslationCacheEntry cached = new TranslationCacheEntry();
        cached.setSourceHash(hashOf("안녕하세요"));
        cached.setTranslatedText("Hello");
        cached.setStatus("SUCCESS");
        when(translationCacheMapper.find("news_feed", "1", "title", "en"))
                .thenReturn(cached);

        String result = service.translate(
                "news_feed", "1", "title", "안녕하세요", ContentLanguage.ENGLISH);

        assertThat(result).isEqualTo("Hello");
        verify(textTranslator, never()).translate(anyString(), any());
    }

    @Test
    void translatesOnceAndStoresSuccessfulResult() {
        when(translationCacheMapper.find("news_feed", "1", "title", "en"))
                .thenReturn(null);
        when(translationCacheMapper.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1);
        when(textTranslator.translate("안녕하세요", ContentLanguage.ENGLISH)).thenReturn("Hello");

        String result = service.translate(
                "news_feed", "1", "title", "안녕하세요", ContentLanguage.ENGLISH);

        assertThat(result).isEqualTo("Hello");
        verify(translationCacheMapper).markSuccess(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void failedRequestReturnsKoreanSourceInsteadOfZeroOrEmptyValue() {
        when(translationCacheMapper.find(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        when(translationCacheMapper.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1);
        when(textTranslator.translate("원문", ContentLanguage.JAPANESE))
                .thenThrow(new IllegalStateException("network failure"));

        String result = service.translate(
                "place", "7", "description", "원문", ContentLanguage.JAPANESE);

        assertThat(result).isEqualTo("원문");
        assertThat(result).isNotBlank().isNotEqualTo("0");
        verify(translationCacheMapper).markFailure(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(LocalDateTime.class), anyString());
        verify(translationCacheMapper, never()).markSuccess(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void retryBlockedFailureDoesNotCallAwsAgain() {
        TranslationCacheEntry failed = new TranslationCacheEntry();
        failed.setSourceHash(hashOf("원문"));
        failed.setStatus("FAILED");
        failed.setRetryAfter(LocalDateTime.now(CLOCK).plusMinutes(1));
        when(translationCacheMapper.find("place", "7", "description", "ja"))
                .thenReturn(failed);

        String result = service.translate(
                "place", "7", "description", "원문", ContentLanguage.JAPANESE);

        assertThat(result).isEqualTo("원문");
        verify(translationCacheMapper, never()).claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
        verify(textTranslator, never()).translate(anyString(), any());
    }

    @Test
    void changedSourceHashDoesNotReusePreviousSuccessfulTranslation() {
        TranslationCacheEntry stale = new TranslationCacheEntry();
        stale.setSourceHash(hashOf("이전 원문"));
        stale.setTranslatedText("Old translation");
        stale.setStatus("SUCCESS");
        when(translationCacheMapper.find("news_feed", "1", "title", "en"))
                .thenReturn(stale);
        when(translationCacheMapper.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1);
        when(textTranslator.translate("새로운 원문", ContentLanguage.ENGLISH))
                .thenReturn("New translation");

        String result = service.translate(
                "news_feed", "1", "title", "새로운 원문", ContentLanguage.ENGLISH);

        assertThat(result).isEqualTo("New translation");
        verify(textTranslator).translate("새로운 원문", ContentLanguage.ENGLISH);
    }

    @Test
    void koreanAndDisabledTranslationDoNotTouchCache() {
        assertThat(service.translate(
                "course", "1", "name", "한국 코스", ContentLanguage.KOREAN))
                .isEqualTo("한국 코스");
        properties.setEnabled(false);
        assertThat(service.translate(
                "course", "1", "name", "한국 코스", ContentLanguage.ENGLISH))
                .isEqualTo("한국 코스");

        verify(translationCacheMapper, never()).find(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void translatesChineseCommunitySourceIntoKorean() {
        when(translationCacheMapper.find(
                "community_post_multilingual", "148", "title", "ko"))
                .thenReturn(null);
        when(translationCacheMapper.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1);
        when(textTranslator.translate(
                "弘大潮流打卡路线",
                ContentLanguage.CHINESE,
                ContentLanguage.KOREAN))
                .thenReturn("홍대 트렌드 인증 코스");

        String result = service.translateMultilingualSource(
                "community_post_multilingual",
                "148",
                "title",
                "弘大潮流打卡路线",
                ContentLanguage.KOREAN);

        assertThat(result).isEqualTo("홍대 트렌드 인증 코스");
        verify(textTranslator).translate(
                "弘大潮流打卡路线",
                ContentLanguage.CHINESE,
                ContentLanguage.KOREAN);
    }

    @Test
    void doesNotRetranslateCommunitySourceAlreadyInTargetLanguage() {
        String result = service.translateMultilingualSource(
                "community_post_multilingual",
                "146",
                "title",
                "Seoul Trend Starter!",
                ContentLanguage.ENGLISH);

        assertThat(result).isEqualTo("Seoul Trend Starter!");
        verify(translationCacheMapper, never()).find(
                anyString(), anyString(), anyString(), anyString());
        verify(textTranslator, never()).translate(
                anyString(), any(ContentLanguage.class), any(ContentLanguage.class));
    }

    @Test
    void fallsBackToBoundedMemoryCacheWhenKoreanCacheSchemaIsNotUpdatedYet() {
        when(translationCacheMapper.find(
                "community_post_multilingual", "148", "title", "ko"))
                .thenThrow(new IllegalStateException("ko constraint not migrated"));
        when(textTranslator.translate(
                "弘大潮流打卡路线",
                ContentLanguage.CHINESE,
                ContentLanguage.KOREAN))
                .thenReturn("홍대 트렌드 인증 코스");

        String first = service.translateMultilingualSource(
                "community_post_multilingual",
                "148",
                "title",
                "弘大潮流打卡路线",
                ContentLanguage.KOREAN);
        String second = service.translateMultilingualSource(
                "community_post_multilingual",
                "148",
                "title",
                "弘大潮流打卡路线",
                ContentLanguage.KOREAN);

        assertThat(first).isEqualTo("홍대 트렌드 인증 코스");
        assertThat(second).isEqualTo(first);
        verify(translationCacheMapper).find(
                "community_post_multilingual", "148", "title", "ko");
        verify(textTranslator).translate(
                "弘大潮流打卡路线",
                ContentLanguage.CHINESE,
                ContentLanguage.KOREAN);
    }

    private String hashOf(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
