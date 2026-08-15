package com.ditto.news.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 뉴스피드 자동 생성 파이프라인 주제 설정.
 * {@code news-feed.generation.*} 애플리케이션 프로퍼티를 바인딩합니다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "news-feed.generation")
public class NewsFeedGenerationProperties {

    /**
     * DITTO가 자동 생성할 K-컬처 뉴스피드 주제 목록.
     */
    private List<String> topics = new ArrayList<>();
}
