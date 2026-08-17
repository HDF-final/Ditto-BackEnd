package com.ditto.news.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 뉴스피드 자동 생성 파이프라인 및 스케줄러 설정.
 * {@code news-feed.generation.*} 애플리케이션 프로퍼티를 바인딩합니다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "news-feed.generation")
public class NewsFeedGenerationProperties {

    /**
     * 스케줄러 활성화 여부 (기본값: true).
     */
    private boolean schedulerEnabled = true;

    /**
     * 스케줄러 실행 크론 표현식 (기본값: 매일 오전 6시 1회 실행).
     */
    private String cron = "0 0 6 * * *";

    /**
     * 실행 1회당 생성할 최대 독립 뉴스피드 개수 (기본값: 3개).
     */
    private int maxFeedsPerTopic = 3;

    /**
     * DITTO가 자동 생성할 K-컬처 뉴스피드 주제 목록.
     */
    private List<String> topics = new ArrayList<>();
}
