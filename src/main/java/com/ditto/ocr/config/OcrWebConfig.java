package com.ditto.ocr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ditto.ocr.support.OcrRateLimitInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * OCR 웹 계층 설정. 레이트 리밋 인터셉터를 인식 엔드포인트에만 건다.
 *
 * <p>CLOVA(유료) 호출이 있는 인식 경로만 대상으로 한다. 세션 시작 등 다른 경로는 제외.
 */
@Configuration
@RequiredArgsConstructor
public class OcrWebConfig implements WebMvcConfigurer {

    private final OcrRateLimitInterceptor ocrRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ocrRateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/ocr/recognitions",
                        "/api/v1/ocr/locations/recognize");
    }
}
