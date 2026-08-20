package com.ditto.global.i18n;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentLanguage {

    KOREAN("ko"),
    CHINESE("zh"),
    JAPANESE("ja"),
    ENGLISH("en");

    private final String code;

    public boolean requiresTranslation() {
        return this != KOREAN;
    }

    public static ContentLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return KOREAN;
        }
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(KOREAN);
    }
}
