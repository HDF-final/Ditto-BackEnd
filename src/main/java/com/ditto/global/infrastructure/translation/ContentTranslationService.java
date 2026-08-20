package com.ditto.global.infrastructure.translation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.repository.TranslationCacheEntry;
import com.ditto.global.infrastructure.translation.repository.TranslationCacheMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContentTranslationService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final TranslationCacheMapper translationCacheMapper;
    private final TextTranslator textTranslator;
    private final TranslationProperties properties;
    private final Clock clock;

    public ContentTranslationService(
            TranslationCacheMapper translationCacheMapper,
            TextTranslator textTranslator,
            TranslationProperties properties,
            @Qualifier("translationClock") Clock clock) {
        this.translationCacheMapper = translationCacheMapper;
        this.textTranslator = textTranslator;
        this.properties = properties;
        this.clock = clock;
    }

    public String translate(
            String sourceType,
            String sourceKey,
            String sourceField,
            String sourceText,
            ContentLanguage targetLanguage) {
        if (!properties.isEnabled()
                || targetLanguage == null
                || !targetLanguage.requiresTranslation()
                || !StringUtils.hasText(sourceText)) {
            return sourceText;
        }

        String sourceHash = hash(sourceText);
        LocalDateTime now = LocalDateTime.now(clock);
        TranslationCacheEntry cached = null;
        boolean claimed = false;

        try {
            cached = translationCacheMapper.find(
                    sourceType, sourceKey, sourceField, targetLanguage.getCode());
            if (cached != null && cached.isSuccessfulFor(sourceHash)) {
                return cached.getTranslatedText();
            }
            if (cached != null && (cached.isPendingFor(sourceHash, now)
                    || cached.isRetryBlockedFor(sourceHash, now))) {
                return sourceText;
            }

            translationCacheMapper.ensureCacheRow(
                    sourceType, sourceKey, sourceField, targetLanguage.getCode(), sourceHash);
            claimed = translationCacheMapper.claim(
                    sourceType,
                    sourceKey,
                    sourceField,
                    targetLanguage.getCode(),
                    sourceHash,
                    Math.max(1, properties.getPendingLease().toSeconds())) == 1;

            if (!claimed) {
                TranslationCacheEntry refreshed = translationCacheMapper.find(
                        sourceType, sourceKey, sourceField, targetLanguage.getCode());
                return refreshed != null && refreshed.isSuccessfulFor(sourceHash)
                        ? refreshed.getTranslatedText()
                        : sourceText;
            }

            String translatedText = textTranslator.translate(sourceText, targetLanguage);
            if (!StringUtils.hasText(translatedText)) {
                markFailure(
                        sourceType, sourceKey, sourceField, targetLanguage, sourceHash,
                        nextRetryAt(now, previousFailureCount(cached, sourceHash)),
                        "EMPTY_TRANSLATION_RESULT");
                return sourceText;
            }

            translationCacheMapper.markSuccess(
                    sourceType,
                    sourceKey,
                    sourceField,
                    targetLanguage.getCode(),
                    sourceHash,
                    translatedText);
            return translatedText;
        } catch (Exception exception) {
            if (claimed) {
                safelyMarkFailure(
                        sourceType,
                        sourceKey,
                        sourceField,
                        targetLanguage,
                        sourceHash,
                        nextRetryAt(now, previousFailureCount(cached, sourceHash)),
                        exception.getClass().getSimpleName());
            }
            log.warn(
                    "동적 콘텐츠 번역 실패. sourceType={}, sourceKey={}, sourceField={}, targetLanguage={}, cause={}",
                    sourceType,
                    sourceKey,
                    sourceField,
                    targetLanguage.getCode(),
                    exception.getClass().getSimpleName());
            return sourceText;
        }
    }

    private void safelyMarkFailure(
            String sourceType,
            String sourceKey,
            String sourceField,
            ContentLanguage targetLanguage,
            String sourceHash,
            LocalDateTime retryAfter,
            String lastError) {
        try {
            markFailure(
                    sourceType, sourceKey, sourceField, targetLanguage, sourceHash, retryAfter, lastError);
        } catch (Exception cacheException) {
            log.warn(
                    "번역 실패 상태 저장 실패. sourceType={}, sourceKey={}, sourceField={}, targetLanguage={}, cause={}",
                    sourceType,
                    sourceKey,
                    sourceField,
                    targetLanguage.getCode(),
                    cacheException.getClass().getSimpleName());
        }
    }

    private void markFailure(
            String sourceType,
            String sourceKey,
            String sourceField,
            ContentLanguage targetLanguage,
            String sourceHash,
            LocalDateTime retryAfter,
            String lastError) {
        translationCacheMapper.markFailure(
                sourceType,
                sourceKey,
                sourceField,
                targetLanguage.getCode(),
                sourceHash,
                retryAfter,
                abbreviate(lastError));
    }

    private LocalDateTime nextRetryAt(LocalDateTime now, int previousFailureCount) {
        long baseSeconds = Math.max(1, properties.getRetryBase().toSeconds());
        long maxSeconds = Math.max(baseSeconds, properties.getRetryMax().toSeconds());
        int exponent = Math.min(Math.max(previousFailureCount, 0), 20);
        long multiplier = 1L << exponent;
        long delaySeconds = baseSeconds > maxSeconds / multiplier
                ? maxSeconds
                : Math.min(baseSeconds * multiplier, maxSeconds);
        return now.plusSeconds(delaySeconds);
    }

    private int previousFailureCount(TranslationCacheEntry cached, String sourceHash) {
        return cached != null && sourceHash.equals(cached.getSourceHash())
                ? cached.getFailureCount()
                : 0;
    }

    private String hash(String sourceText) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String abbreviate(String value) {
        String safeValue = value == null ? "UNKNOWN" : value;
        return safeValue.length() <= MAX_ERROR_LENGTH
                ? safeValue
                : safeValue.substring(0, MAX_ERROR_LENGTH);
    }
}
