package com.ditto.ocr.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ditto.ocr.support.OcrRateLimitInterceptor;
import com.ditto.ocr.support.OcrRateLimiter;

import lombok.RequiredArgsConstructor;

/**
 * OCR 웹 계층 설정. 레이트 리밋 인터셉터를 인식 엔드포인트에만 건다.
 *
 * <p>CLOVA(유료) 호출이 있는 인식 경로만 대상으로 한다. 세션 시작 등 다른 경로는 제외.
 *
 * <p>{@link OcrRateLimiter} 는 {@link ObjectProvider} 로 지연 조회해 인터셉터를 직접 만든다.
 * 이렇게 하면 이 설정이 {@code @WebMvcTest} 슬라이스(레이트 리미터 빈이 없는 컨텍스트)에서도
 * 생성 실패 없이 로드되고, 빈이 없으면 인터셉터 등록만 건너뛴다. 운영 컨텍스트에서는 정상 등록된다.
 */
@Configuration
@RequiredArgsConstructor
public class OcrWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<OcrRateLimiter> rateLimiterProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        OcrRateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
        if (rateLimiter == null) {
            return;
        }
        registry.addInterceptor(new OcrRateLimitInterceptor(rateLimiter))
                .addPathPatterns(
                        "/api/v1/ocr/recognitions",
                        "/api/v1/ocr/locations/recognize");
    }
}
