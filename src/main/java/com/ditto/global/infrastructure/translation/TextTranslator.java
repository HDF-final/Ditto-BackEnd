package com.ditto.global.infrastructure.translation;

import com.ditto.global.i18n.ContentLanguage;

public interface TextTranslator {

    String translate(String sourceText, ContentLanguage targetLanguage);

    default String translate(
            String sourceText,
            ContentLanguage sourceLanguage,
            ContentLanguage targetLanguage) {
        if (sourceLanguage == ContentLanguage.KOREAN) {
            return translate(sourceText, targetLanguage);
        }
        throw new UnsupportedOperationException("Source language selection is not supported");
    }
}
