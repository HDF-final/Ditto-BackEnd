package com.ditto.ocr.domain;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OCR 길찾기 세션.
 *
 * <p>세션 시작(placeId·source 발급)과 인식 요청 사이를 잇는 짧은 수명의 상태다.
 * 지금은 인메모리로만 들고 있고, 다중 인스턴스로 확장되면 Redis 로 옮긴다.
 */
@Getter
@RequiredArgsConstructor
public class OcrSession {

    private final String sessionId;
    private final Long placeId;
    private final String source;
    private final Instant createdAt;

    public boolean isExpired(Instant now, java.time.Duration ttl) {
        return createdAt.plus(ttl).isBefore(now);
    }
}
