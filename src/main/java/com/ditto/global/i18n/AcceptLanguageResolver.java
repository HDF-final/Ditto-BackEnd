package com.ditto.global.i18n;

import java.util.List;
import java.util.Locale;

public final class AcceptLanguageResolver {

    private AcceptLanguageResolver() {
    }

    public static ContentLanguage resolve(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return ContentLanguage.KOREAN;
        }

        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
            for (Locale.LanguageRange range : ranges) {
                if (range.getWeight() <= 0 || "*".equals(range.getRange())) {
                    continue;
                }
                String languageCode = Locale.forLanguageTag(range.getRange()).getLanguage();
                ContentLanguage language = ContentLanguage.fromCode(languageCode);
                if (!ContentLanguage.KOREAN.equals(language) || "ko".equalsIgnoreCase(languageCode)) {
                    return language;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return ContentLanguage.KOREAN;
        }
        return ContentLanguage.KOREAN;
    }
}
