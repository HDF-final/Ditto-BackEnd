package com.ditto.news.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ditto.news.application.port.out.AiNewsFeedGenerator;
import com.ditto.news.application.port.out.NewsArticleCollector;
import com.ditto.news.application.port.out.NewsArticleCrawler;
import com.ditto.news.application.port.out.NewsArticleSelector;
import com.ditto.news.config.NewsFeedGenerationProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스피드 생성 파이프라인 서비스.
 * RSS 후보 수집 -> Python 기사 본문 크롤링 -> 가중치 선별 -> AI 뉴스피드 생성을 순차적으로 오케스트레이션합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFeedPipelineService {

    private final NewsArticleCollector collector;
    private final NewsArticleCrawler crawler;
    private final NewsArticleSelector selector;
    private final AiNewsFeedGenerator aiNewsFeedGenerator;
    private final NewsFeedGenerationProperties properties;

    /**
     * 특정 토픽에 대해 파이프라인을 실행하고 최종 생성된 뉴스피드를 반환합니다.
     */
    public GeneratedNewsFeed executePipeline(String topic) {
        NewsPipelineDebugResponse debugResponse = executePipelineWithDebug(topic);
        return debugResponse != null ? debugResponse.getGeneratedFeed() : null;
    }

    /**
     * 디버그/검증용: 각 파이프라인 단계별 메타데이터를 포함한 응답을 반환합니다.
     */
    public NewsPipelineDebugResponse executePipelineWithDebug(String topic) {
        if (topic == null || topic.isBlank()) {
            log.warn("토픽이 지정되지 않아 뉴스피드 생성을 건너뜁니다.");
            return null;
        }

        log.info("[뉴스 파이프라인 시작] topic={}", topic);

        // [1단계] 기사 후보 수집
        List<NewsArticleCandidate> candidates = collector.collect(topic);
        int candidateCount = candidates.size();
        if (candidates.isEmpty()) {
            log.info("[뉴스 파이프라인 중단] 수집된 기사 후보가 없습니다. topic={}", topic);
            return emptyDebugResponse(topic, 0, 0, 0);
        }

        // [2단계] 기사 본문 크롤링
        List<CrawledNewsArticle> crawledArticles = crawler.crawlAll(candidates);
        int crawledCount = crawledArticles.size();
        if (crawledArticles.isEmpty()) {
            log.warn("[뉴스 파이프라인 중단] 본문 크롤링 성공 건수가 없습니다. topic={}", topic);
            return emptyDebugResponse(topic, candidateCount, 0, 0);
        }

        // [3단계] 기사 선별 및 랭킹
        List<CrawledNewsArticle> selectedArticles = selector.selectRelevantArticles(crawledArticles, List.of(topic));
        int selectedCount = selectedArticles.size();
        if (selectedArticles.isEmpty()) {
            log.info("[뉴스 파이프라인 중단] 선별 기준을 만족하는 기사가 없습니다. topic={}", topic);
            return emptyDebugResponse(topic, candidateCount, crawledCount, 0);
        }

        // [4단계] AI 뉴스피드 콘텐츠 2차 생성 (Gemini LLM)
        GeneratedNewsFeed generatedFeed = aiNewsFeedGenerator.generate(selectedArticles, topic);

        log.info("[뉴스 파이프라인 완료] topic={}, candidates={}, crawled={}, selected={}, title='{}'",
                topic, candidateCount, crawledCount, selectedCount,
                generatedFeed != null ? generatedFeed.getTitle() : "null");

        return NewsPipelineDebugResponse.builder()
                .topic(topic)
                .candidateCount(candidateCount)
                .crawledCount(crawledCount)
                .selectedCount(selectedCount)
                .selectedArticles(selectedArticles)
                .generatedFeed(generatedFeed)
                .build();
    }

    /**
     * 설정된 모든 토픽에 대해 순차적으로 파이프라인을 실행합니다.
     */
    public List<GeneratedNewsFeed> executeAllTopics() {
        List<String> topics = properties.getTopics();
        if (topics == null || topics.isEmpty()) {
            log.warn("설정된 뉴스피드 생성 대상 토픽이 없습니다.");
            return Collections.emptyList();
        }

        List<GeneratedNewsFeed> results = new ArrayList<>();
        for (String topic : topics) {
            try {
                GeneratedNewsFeed feed = executePipeline(topic);
                if (feed != null) {
                    results.add(feed);
                }
            } catch (Exception e) {
                log.error("토픽({}) 뉴스피드 생성 실패: cause={}", topic, e.getMessage(), e);
            }
        }
        return results;
    }

    private NewsPipelineDebugResponse emptyDebugResponse(String topic, int candidates, int crawled, int selected) {
        return NewsPipelineDebugResponse.builder()
                .topic(topic)
                .candidateCount(candidates)
                .crawledCount(crawled)
                .selectedCount(selected)
                .selectedArticles(Collections.emptyList())
                .generatedFeed(null)
                .build();
    }
}
