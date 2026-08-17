package com.ditto.news.outbound.collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * RSS 및 Atom 피드의 다양한 발행 일시 문자열을 Asia/Seoul 기준의 {@link LocalDateTime}으로 파싱하는 유틸리티.
 */
@Slf4j
public final class RssDateParser {

    private static final ZoneId TARGET_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter RFC_NO_DOW = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d[d] MMM yyyy HH:mm[:ss][ zzz][ Z][ z]")
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter[] FALLBACK_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    private RssDateParser() {
    }

    /**
     * 문자열 날짜를 Asia/Seoul 기준의 LocalDateTime으로 안전하게 변환합니다.
     * 파싱 실패 시 예외를 던지지 않고 null을 반환합니다.
     *
     * @param dateStr RSS/Atom pubDate, published, dc:date 등 원본 문자열
     * @return 파싱된 LocalDateTime 또는 null
     */
    public static LocalDateTime parse(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        String trimmed = dateStr.trim();

        // 1. 표준 RFC-1123 시도
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.withZoneSameInstant(TARGET_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        // 2. DayOfWeek 불일치 및 변형 대응: 요일 접두사 제거 후 RFC 날짜 파싱 시도
        String withoutDow = trimmed.replaceFirst("^[A-Za-z]{3},\\s*", "");
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(withoutDow, RFC_NO_DOW);
            return zdt.withZoneSameInstant(TARGET_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        // 3. 표준 ISO-8601 (Offset / Instant) 시도
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return zdt.withZoneSameInstant(TARGET_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            Instant instant = Instant.parse(trimmed);
            return LocalDateTime.ofInstant(instant, TARGET_ZONE);
        } catch (DateTimeParseException ignored) {
        }

        // 4. 대체 포맷터 순회 시도
        for (DateTimeFormatter formatter : FALLBACK_FORMATTERS) {
            try {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(trimmed, formatter);
                    return zdt.withZoneSameInstant(TARGET_ZONE).toLocalDateTime();
                } catch (Exception e) {
                    return LocalDateTime.parse(trimmed, formatter);
                }
            } catch (Exception ignored) {
            }
        }

        log.debug("RSS 발행일시 파싱 실패 (null 반환): rawDate={}", trimmed);
        return null;
    }
}
