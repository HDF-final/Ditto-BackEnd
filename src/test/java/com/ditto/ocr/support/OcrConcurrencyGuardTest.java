package com.ditto.ocr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.config.OcrProperties;

/**
 * 벌크헤드가 동시 처리 상한을 실제로 강제하는지 검증한다.
 * 상한만큼은 통과시키고, 초과분은 즉시 503(OCR_TOO_MANY_REQUESTS)으로 반려해야 한다.
 */
class OcrConcurrencyGuardTest {

    private OcrConcurrencyGuard guard(int maxConcurrent, Duration acquireTimeout) {
        OcrProperties properties = new OcrProperties();
        properties.getConcurrency().setMaxConcurrent(maxConcurrent);
        properties.getConcurrency().setAcquireTimeout(acquireTimeout);
        return new OcrConcurrencyGuard(properties);
    }

    @Test
    @DisplayName("동시 상한을 초과한 요청은 즉시 503으로 반려된다")
    void rejectsWhenOverCapacity() throws Exception {
        OcrConcurrencyGuard guard = guard(2, Duration.ofMillis(100));

        CountDownLatch bothInside = new CountDownLatch(2); // 슬롯 2개가 모두 점유됨
        CountDownLatch release = new CountDownLatch(1);     // 점유 유지용

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 슬롯 2개를 붙잡고 대기하는 작업 2개
            Future<Boolean> f1 = pool.submit(() -> guard.call(() -> hold(bothInside, release)));
            Future<Boolean> f2 = pool.submit(() -> guard.call(() -> hold(bothInside, release)));

            // 두 작업이 모두 슬롯을 점유할 때까지 대기
            assertThat(bothInside.await(2, TimeUnit.SECONDS)).isTrue();

            // 3번째 요청: 남은 슬롯이 없어 acquireTimeout(100ms) 후 503
            assertThatThrownBy(() -> guard.call(() -> "should-not-run"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.OCR_TOO_MANY_REQUESTS);

            // 점유 해제 → 앞선 2건은 정상 완료
            release.countDown();
            assertThat(f1.get(2, TimeUnit.SECONDS)).isTrue();
            assertThat(f2.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("슬롯이 반납되면 이후 요청은 다시 통과한다")
    void allowsAfterRelease() {
        OcrConcurrencyGuard guard = guard(1, Duration.ofMillis(50));

        String first = guard.call(() -> "ok-1");
        String second = guard.call(() -> "ok-2"); // 앞 요청이 끝나 슬롯 반납됨

        assertThat(first).isEqualTo("ok-1");
        assertThat(second).isEqualTo("ok-2");
    }

    private boolean hold(CountDownLatch bothInside, CountDownLatch release) {
        bothInside.countDown();
        try {
            release.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }
}
