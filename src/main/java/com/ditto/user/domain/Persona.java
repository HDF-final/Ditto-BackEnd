package com.ditto.user.domain;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Persona {

    OPEN_RUN_LOVER("오픈런러버", "OPEN-RUN LOVER", "신상·팝업 뜨면 제일 먼저"),
    FLEX_SPENDER("플렉스족", "FLEX SPENDER", "명품·프리미엄 제대로"),
    LITTLE_JOY("소확행러버", "LITTLE JOY", "카페·디저트·감성 소품"),
    ULTIMATE_STAN("최애덕후", "ULTIMATE STAN", "K팝 최애 굿즈·앨범 덕질");

    private final String displayName;
    private final String englishName;
    private final String description;

    public static Persona from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(value.trim()) || p.displayName.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
