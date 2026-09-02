package com.ditto.ocr.support;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.config.OcrProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * OCR 무거운 구간(이미지 디코딩 + CLOVA 호출)의 동시 실행 수를 제한하는 벌크헤드.
 *
 * <p>외부 호출은 요청 스레드를 최대 수 초 붙잡는다. 동시 진입 수를 Tomcat 워커 풀보다
 * 작게 묶어, 폭주 시에도 OCR 이 전체 스레드를 먹고 앱 전체를 멈추게 하는 일을 막는다.
 * 초과 요청은 짧게만 대기하다 {@link ErrorCode#OCR_TOO_MANY_REQUESTS}(503) 로 즉시 반려해
 * 워커 스레드를 곧바로 반납한다.
 */
@Slf4j
@Component
public class OcrConcurrencyGuard {

    private final Semaphore semaphore;
    private final Duration acquireTimeout;

    public OcrConcurrencyGuard(OcrProperties properties) {
        OcrProperties.Concurrency cfg = properties.getConcurrency();
        this.semaphore = new Semaphore(cfg.getMaxConcurrent());
        this.acquireTimeout = cfg.getAcquireTimeout();
    }

    /** 슬롯을 얻으면 작업을 실행하고, 못 얻으면 즉시 503 으로 반려한다. */
    public <T> T call(Supplier<T> task) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OCR_SERVICE_ERROR);
        }
        if (!acquired) {
            log.warn("OCR 동시 처리 한도 초과로 요청 반려. availablePermits={}", semaphore.availablePermits());
            throw new BusinessException(ErrorCode.OCR_TOO_MANY_REQUESTS);
        }
        try {
            return task.get();
        } finally {
            semaphore.release();
        }
    }
}
