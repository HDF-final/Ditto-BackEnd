package com.ditto.global.infrastructure.translation;

import com.ditto.global.i18n.ContentLanguage;

public interface TextTranslator {

    String translate(String sourceText, ContentLanguage targetLanguage);
}
