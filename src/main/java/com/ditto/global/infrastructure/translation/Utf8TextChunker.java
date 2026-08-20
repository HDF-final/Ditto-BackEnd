package com.ditto.global.infrastructure.translation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Utf8TextChunker {

    private final TranslationProperties properties;

    public List<String> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        int maxBytes = Math.max(4, properties.getMaxRequestBytes());
        if (utf8Length(text) <= maxBytes) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = findEnd(text, start, maxBytes);
            chunks.add(text.substring(start, end));
            start = end;
        }
        return List.copyOf(chunks);
    }

    private int findEnd(String text, int start, int maxBytes) {
        int cursor = start;
        int bytes = 0;
        int preferredBreak = -1;
        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            int codePointBytes = utf8Length(new String(Character.toChars(codePoint)));
            if (bytes + codePointBytes > maxBytes) {
                break;
            }
            bytes += codePointBytes;
            cursor += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || codePoint == '.' || codePoint == '!' || codePoint == '?'
                    || codePoint == '。' || codePoint == '！' || codePoint == '？') {
                preferredBreak = cursor;
            }
        }

        if (cursor == start) {
            return start + Character.charCount(text.codePointAt(start));
        }
        if (cursor < text.length() && preferredBreak > start) {
            return preferredBreak;
        }
        return cursor;
    }

    private int utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
