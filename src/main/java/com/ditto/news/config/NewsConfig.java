package com.ditto.news.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 뉴스 도메인 관련 설정.
 */
@Configuration
@EnableConfigurationProperties(NewsFeedGenerationProperties.class)
public class NewsConfig {
}
