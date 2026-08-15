package com.ditto.news.crawler;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스 크롤러 기본 네트워크 및 요청 설정값.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
public class CrawlerProperties {

    /** 기본 타임아웃 (connect + read, 밀리초 단위): 5초 */
    @Builder.Default
    private int timeoutMillis = 5000;

    /** 기본 User-Agent 문자열 */
    @Builder.Default
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 (Ditto-NewsCrawler/1.0)";

    /** 최대 응답 바디 크기 (바이트 단위, 기본 5MB) */
    @Builder.Default
    private int maxBodySizeBytes = 5 * 1024 * 1024;

    /** 리다이렉트 자동 추적 여부 */
    @Builder.Default
    private boolean followRedirects = true;

    /** 비정상 HTTP 상태 코드 무시 여부 (false 시 4xx, 5xx에서 HttpStatusException 발생) */
    @Builder.Default
    private boolean ignoreHttpErrors = false;
}
