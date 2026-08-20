package com.ditto.country.domain;

import java.util.Locale;

public enum SupportedCountry {

    KR,
    CN,
    JP,
    US;

    public static SupportedCountry fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
