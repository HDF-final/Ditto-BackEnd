package com.ditto.global.infrastructure.translation.repository;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslationCacheEntry {

    private String sourceHash;
    private String translatedText;
    private String status;
    private int failureCount;
    private LocalDateTime retryAfter;
    private LocalDateTime leaseUntil;

    public boolean isSuccessfulFor(String expectedSourceHash) {
        return expectedSourceHash.equals(sourceHash)
                && "SUCCESS".equals(status)
                && translatedText != null
                && !translatedText.isBlank();
    }

    public boolean isPendingFor(String expectedSourceHash, LocalDateTime now) {
        return expectedSourceHash.equals(sourceHash)
                && "PENDING".equals(status)
                && leaseUntil != null
                && leaseUntil.isAfter(now);
    }

    public boolean isRetryBlockedFor(String expectedSourceHash, LocalDateTime now) {
        return expectedSourceHash.equals(sourceHash)
                && "FAILED".equals(status)
                && retryAfter != null
                && retryAfter.isAfter(now);
    }
}
