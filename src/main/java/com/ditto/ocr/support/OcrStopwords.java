package com.ditto.ocr.support;

import java.util.regex.Pattern;

/**
 * 상호가 될 수 없는 <strong>형태</strong>만 버린다.
 *
 * <p>SALE·세일중 같은 프로모 문구를 사전으로 모아 지우지 않는다. 변형이 무한히 나와서
 * 리스트를 키우는 방식으로는 못 이긴다. 그런 글자는 카탈로그에 없으면 {@code matchScore}
 * 미달로 후보에서 떨어진다.
 *
 * <p>여기 남는 것은 매장명이 절대 아닌 구조다. 층수({@code 1F}), 할인율({@code 50%}),
 * 가격({@code 12,000원}), 시각({@code 10:30}).
 */
public final class OcrStopwords {

    /** 1F, 3F, B1, B2F. 단독 숫자(1985)는 상호에 쓰이므로 제외한다. */
    private static final Pattern FLOOR_MARK = Pattern.compile("^(B\\d+F?|\\d+F)$");

    private static final Pattern PERCENT = Pattern.compile("[0-9]+\\s*[%％퍼]");

    private static final Pattern PRICE = Pattern.compile("([₩￦]\\s*[0-9,]+)|([0-9,]+\\s*원)");

    private static final Pattern CLOCK = Pattern.compile("\\d{1,2}\\s*[:시]\\s*\\d{2}");

    private OcrStopwords() {
    }

    /**
     * 층·가격·할인율·시각처럼 상호가 될 수 없는 형태이면 {@code true}.
     * {@code 세일중}·{@code SALE} 은 false 다. 그건 매칭이 버린다.
     */
    public static boolean isStopword(String text) {
        return isStructuralNoise(text);
    }

    public static boolean isStructuralNoise(String text) {
        if (text == null) {
            return true;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String normalized = OcrTextNormalizer.normalize(trimmed);
        if (normalized.isEmpty()) {
            return true;
        }
        if (FLOOR_MARK.matcher(normalized).matches()) {
            return true;
        }
        return PERCENT.matcher(trimmed).find()
                || PRICE.matcher(trimmed).find()
                || CLOCK.matcher(trimmed).find();
    }
}
