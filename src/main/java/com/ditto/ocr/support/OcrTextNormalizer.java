package com.ditto.ocr.support;

/**
 * OCR 텍스트·상호 정규화.
 *
 * <p>간판 OCR 은 대소문자·띄어쓰기·점·기호가 제각각이라 원문 그대로는 매칭이 잘 안 된다.
 * 글자와 숫자(한글 포함)만 남기고 나머지는 버린 뒤 대문자로 통일해, 매칭 양쪽을 같은 형태로 맞춘다.
 * 예) {@code "EAT ALY."} → {@code "EATALY"}, {@code "탬버린 스"} → {@code "탬버린스"}
 */
public final class OcrTextNormalizer {

    private OcrTextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(Character.toUpperCase(c));
            }
        }
        return builder.toString();
    }
}
