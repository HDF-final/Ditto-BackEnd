package com.ditto.user.domain;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreferredLanguage {

    KOREAN("ko"),
    CHINESE("zh"),
    JAPANESE("ja"),
    ENGLISH("en");

    private final String code;

    public static PreferredLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (PreferredLanguage language : values()) {
            if (language.code.equals(normalized)) {
                return language;
            }
        }
        return null;
    }
}
