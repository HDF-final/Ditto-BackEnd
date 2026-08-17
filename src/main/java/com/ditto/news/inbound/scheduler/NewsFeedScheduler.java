package com.ditto.news.inbound.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.config.NewsFeedGenerationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 설정된 크론 주기에 따라 K-컬처 뉴스피드 자동 생성 파이프라인을 실행하는 인바운드 스케줄러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "news-feed.generation", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NewsFeedScheduler {

    private final NewsFeedPipelineService pipelineService;
    private final NewsFeedGenerationProperties properties;

    /**
     * 주기적으로 뉴스피드 생성 파이프라인을 자동 실행합니다.
     * 기본 주기: 매 3시간마다 실행
     */
    @Scheduled(cron = "${news-feed.generation.cron:0 0 */3 * * *}")
    public void runNewsFeedGenerationSchedule() {
        log.info("⏰ [뉴스피드 자동 스케줄러 시작] 대상 토픽: {}", properties.getTopics());
        List<GeneratedNewsFeed> generatedFeeds = pipelineService.executeAllTopics();
        log.info("⏰ [뉴스피드 자동 스케줄러 종료] 생성된 총 뉴스피드 수: {}건", generatedFeeds.size());
    }
}
