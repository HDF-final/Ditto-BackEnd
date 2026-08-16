package com.ditto.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Python 뉴스 크롤러 서비스(FastAPI) 연동 설정값.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "news.crawler")
public class CrawlerProperties {

    /** Python 크롤러 서비스 Base URL */
    @Builder.Default
    private String serviceUrl = "http://localhost:8000";

    /** 기사 크롤링 API 경로 */
    @Builder.Default
    private String crawlPath = "/crawl";

    /** 연결 타임아웃 (초 단위) */
    @Builder.Default
    private int connectTimeoutSeconds = 5;

    /** 읽기/응답 타임아웃 (초 단위, Selenium 렌더링 고려) */
    @Builder.Default
    private int readTimeoutSeconds = 30;
}
