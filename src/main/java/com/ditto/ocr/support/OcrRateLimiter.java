package com.ditto.ocr.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.ditto.ocr.config.OcrProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * IP(키) 단위 고정 윈도우 레이트 리밋.
 *
 * <p>같은 키로 한 윈도우 안에 {@code limit} 회까지만 허용하고 초과분은 거절한다.
 * 벌크헤드가 "동시 처리 수"를 막는다면, 이쪽은 "일정 시간 누적 요청 수"를 막아
 * 한 클라이언트의 유료 CLOVA 호출 남용을 차단한다.
 *
 * <p>인메모리 단일 인스턴스 기준이다. {@link java.util.concurrent.ConcurrentHashMap#compute}
 * 로 키별 원자 갱신을 하고, 스케줄러에 의존하지 않도록 맵이 일정 크기를 넘으면
 * 만료된 윈도우를 그때 함께 걷어낸다.
 */
@Slf4j
@Component
public class OcrRateLimiter {

    /** 만료 엔트리 지연 정리를 시작하는 맵 크기 임계값. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final OcrProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public OcrRateLimiter(OcrProperties properties) {
        this.properties = properties;
    }

    /** 키의 이번 윈도우 요청 수가 상한 이내면 true, 초과면 false. */
    public boolean tryAcquire(String key) {
        OcrProperties.RateLimit cfg = properties.getRateLimit();
        if (!cfg.isEnabled()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long windowMillis = cfg.getWindow().toMillis();

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.start >= windowMillis);
        }

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMillis) {
                return new Window(now);
            }
            existing.count++;
            return existing;
        });

        boolean allowed = window.count <= cfg.getLimit();
        if (!allowed) {
            log.warn("OCR 레이트 리밋 초과. key={} count={} limit={}", key, window.count, cfg.getLimit());
        }
        return allowed;
    }

    /** 하나의 고정 윈도우. 시작 시각과 그 안에서의 요청 수를 센다. */
    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}
