package com.ditto.ocr.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.ocr.config.OcrProperties;

/**
 * IP 단위 고정 윈도우 레이트 리밋 검증.
 * 윈도우당 상한까지는 허용하고 초과분은 거절하며, 윈도우가 지나면 다시 허용해야 한다.
 */
class OcrRateLimiterTest {

    private OcrRateLimiter limiter(int limit, Duration window, boolean enabled) {
        OcrProperties properties = new OcrProperties();
        properties.getRateLimit().setLimit(limit);
        properties.getRateLimit().setWindow(window);
        properties.getRateLimit().setEnabled(enabled);
        return new OcrRateLimiter(properties);
    }

    @Test
    @DisplayName("윈도우당 상한까지 허용하고 초과분은 거절한다")
    void allowsUpToLimitThenRejects() {
        OcrRateLimiter limiter = limiter(10, Duration.ofMinutes(1), true);

        for (int i = 1; i <= 10; i++) {
            assertThat(limiter.tryAcquire("1.1.1.1")).as("%d번째 요청", i).isTrue();
        }
        assertThat(limiter.tryAcquire("1.1.1.1")).as("11번째 요청은 거절").isFalse();
    }

    @Test
    @DisplayName("서로 다른 IP 는 독립적으로 카운트된다")
    void countsPerKeyIndependently() {
        OcrRateLimiter limiter = limiter(2, Duration.ofMinutes(1), true);

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse(); // A 소진

        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();   // B 는 별도
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
    }

    @Test
    @DisplayName("윈도우가 지나면 카운터가 초기화되어 다시 허용된다")
    void resetsAfterWindow() throws Exception {
        OcrRateLimiter limiter = limiter(1, Duration.ofMillis(50), true);

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse(); // 같은 윈도우 내 초과

        Thread.sleep(80); // 윈도우 경과

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();  // 초기화 후 다시 허용
    }

    @Test
    @DisplayName("비활성화하면 상한과 무관하게 항상 허용한다")
    void disabledAlwaysAllows() {
        OcrRateLimiter limiter = limiter(1, Duration.ofMinutes(1), false);

        for (int i = 0; i < 100; i++) {
            assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        }
    }
}
