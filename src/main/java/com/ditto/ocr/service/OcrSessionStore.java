package com.ditto.ocr.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.config.OcrProperties;
import com.ditto.ocr.domain.OcrSession;

import lombok.RequiredArgsConstructor;

/**
 * OCR 세션 인메모리 스토어.
 *
 * <p>세션은 시작~인식 사이 수 초를 잇는 용도라 인메모리로 충분하다.
 * 만료된 세션은 조회 시점(지연 만료)과 주기적 청소로 함께 걷어낸다.
 * 다중 인스턴스로 확장되면 이 구현만 Redis 기반으로 바꾸면 된다.
 */
@Component
@RequiredArgsConstructor
public class OcrSessionStore {

    private final OcrProperties properties;
    private final Map<String, OcrSession> sessions = new ConcurrentHashMap<>();

    /** 세션을 만들고 발급한 sessionId 를 돌려준다. */
    public OcrSession create(Long placeId, String source) {
        String sessionId = "session_" + java.util.UUID.randomUUID().toString().replace("-", "");
        OcrSession session = new OcrSession(sessionId, placeId, source, Instant.now());
        sessions.put(sessionId, session);
        return session;
    }

    /** 유효한 세션을 조회한다. 없거나 만료됐으면 {@link ErrorCode#OCR_SESSION_NOT_FOUND}. */
    public OcrSession require(String sessionId) {
        OcrSession session = sessionId == null ? null : sessions.get(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.OCR_SESSION_NOT_FOUND);
        }
        if (session.isExpired(Instant.now(), properties.getSessionTtl())) {
            sessions.remove(sessionId);
            throw new BusinessException(ErrorCode.OCR_SESSION_NOT_FOUND);
        }
        return session;
    }

    /** 만료 세션 주기적 청소. 조회되지 않고 버려진 세션의 메모리 누수를 막는다. */
    @Scheduled(fixedDelay = 600_000L)
    public void evictExpired() {
        Instant now = Instant.now();
        Duration ttl = properties.getSessionTtl();
        sessions.values().removeIf(session -> session.isExpired(now, ttl));
    }
}
