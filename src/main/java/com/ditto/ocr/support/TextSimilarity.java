package com.ditto.ocr.support;

/**
 * 정규화된 문자열 간 유사도.
 *
 * <p>간판 텍스트가 상호에 그대로 포함되면 확실한 매칭(1.0)으로 보고, 그렇지 않으면
 * 편집 거리(Levenshtein) 기반 비율로 근사한다. OCR 이 한두 글자 오인식해도 근접 매칭이 살아난다.
 */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    /**
     * 두 정규화 문자열의 유사도(0~1). 포함 관계면 1.0, 아니면 편집 거리 비율.
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return 1.0;
        }
        int distance = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        return 1.0 - (double) distance / max;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
