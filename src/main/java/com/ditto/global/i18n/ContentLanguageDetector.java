package com.ditto.global.i18n;

public final class ContentLanguageDetector {

    private ContentLanguageDetector() {
    }

    public static ContentLanguage detect(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int hangulCount = 0;
        int kanaCount = 0;
        int hanCount = 0;
        int latinCount = 0;

        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);

            if (script == Character.UnicodeScript.HANGUL) {
                hangulCount++;
            } else if (script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA) {
                kanaCount++;
            } else if (script == Character.UnicodeScript.HAN) {
                hanCount++;
            } else if (script == Character.UnicodeScript.LATIN
                    && Character.isLetter(codePoint)) {
                latinCount++;
            }

            offset += Character.charCount(codePoint);
        }

        if (hangulCount > 0) {
            return ContentLanguage.KOREAN;
        }
        if (kanaCount > 0) {
            return ContentLanguage.JAPANESE;
        }
        if (hanCount > 0) {
            return ContentLanguage.CHINESE;
        }
        if (latinCount > 0) {
            return ContentLanguage.ENGLISH;
        }
        return null;
    }
}
