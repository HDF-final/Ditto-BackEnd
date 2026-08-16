package com.ditto.news.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.news.application.port.out.AiNewsFeedGenerator;
import com.ditto.news.application.port.out.NewsArticleCollector;
import com.ditto.news.application.port.out.NewsArticleCrawler;
import com.ditto.news.application.port.out.NewsArticleSelector;
import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.config.NewsFeedGenerationProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;

@ExtendWith(MockitoExtension.class)
class NewsFeedPipelineServiceTest {

    @Mock
    private NewsArticleCollector collector;

    @Mock
    private NewsArticleCrawler crawler;

    @Mock
    private NewsArticleSelector selector;

    @Mock
    private AiNewsFeedGenerator aiNewsFeedGenerator;

    @Mock
    private NewsFeedRepository newsFeedRepository;

    private NewsFeedGenerationProperties properties;
    private NewsFeedPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        properties = new NewsFeedGenerationProperties();
        properties.setTopics(List.of("K-POP", "K-Drama"));
        pipelineService = new NewsFeedPipelineService(collector, crawler, selector, aiNewsFeedGenerator, newsFeedRepository, properties);
    }

    @Test
    @DisplayName("후보수집 -> 본문크롤링 -> 기사선별 -> AI생성 -> DB저장 5단계 전 파이프라인이 순차적으로 정상 실행된다")
    void executesPipelineSuccessfully() {
        String topic = "K-POP";
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("K-POP Single")
                .url("https://www.yna.co.kr/view/1")
                .build();

        CrawledNewsArticle crawled = CrawledNewsArticle.builder()
                .title("K-POP Single")
                .body("Article body content")
                .url("https://www.yna.co.kr/view/1")
                .build();

        GeneratedNewsFeed expectedFeed = GeneratedNewsFeed.builder()
                .title("[K-POP] K-POP Single")
                .body("Summary")
                .summaries(List.of("요약 1", "요약 2", "요약 3"))
                .keywords(List.of("#KPOP"))
                .build();

        NewsFeed savedFeed = NewsFeed.builder()
                .newsFeedId(101L)
                .title("[K-POP] K-POP Single")
                .build();

        given(collector.collect(topic)).willReturn(List.of(candidate));
        given(crawler.crawlAll(List.of(candidate))).willReturn(List.of(crawled));
        given(selector.selectRelevantArticles(List.of(crawled), List.of(topic))).willReturn(List.of(crawled));
        given(aiNewsFeedGenerator.generate(List.of(crawled), topic)).willReturn(expectedFeed);
        given(newsFeedRepository.save(expectedFeed)).willReturn(savedFeed);

        NewsPipelineDebugResponse debugResult = pipelineService.executePipelineWithDebug(topic);

        assertThat(debugResult).isNotNull();
        assertThat(debugResult.getGeneratedFeed().getTitle()).isEqualTo("[K-POP] K-POP Single");
        assertThat(debugResult.getSavedNewsFeedId()).isEqualTo(101L);

        verify(collector).collect(topic);
        verify(crawler).crawlAll(List.of(candidate));
        verify(selector).selectRelevantArticles(List.of(crawled), List.of(topic));
        verify(aiNewsFeedGenerator).generate(List.of(crawled), topic);
        verify(newsFeedRepository).save(expectedFeed);
    }

    @Test
    @DisplayName("후보 기사가 0건이면 후속 크롤링, 생성 및 DB 저장을 건너뛰고 null을 반환한다")
    void skipsPipelineWhenNoCandidates() {
        String topic = "K-POP";
        given(collector.collect(topic)).willReturn(Collections.emptyList());

        GeneratedNewsFeed result = pipelineService.executePipeline(topic);

        assertThat(result).isNull();
        verify(crawler, never()).crawlAll(any());
        verify(aiNewsFeedGenerator, never()).generate(any(), any());
        verify(newsFeedRepository, never()).save(any());
    }

    @Test
    @DisplayName("executeAllTopics는 설정된 모든 토픽에 대해 파이프라인을 실행한다")
    void executesAllConfiguredTopics() {
        NewsArticleCandidate candidate = NewsArticleCandidate.builder().url("https://url.com").build();
        CrawledNewsArticle crawled = CrawledNewsArticle.builder().url("https://url.com").build();
        GeneratedNewsFeed feed = GeneratedNewsFeed.builder().title("Feed").build();

        given(collector.collect(any())).willReturn(List.of(candidate));
        given(crawler.crawlAll(any())).willReturn(List.of(crawled));
        given(selector.selectRelevantArticles(any(), any())).willReturn(List.of(crawled));
        given(aiNewsFeedGenerator.generate(any(), any())).willReturn(feed);

        List<GeneratedNewsFeed> results = pipelineService.executeAllTopics();

        assertThat(results).hasSize(2);
        verify(collector).collect("K-POP");
        verify(collector).collect("K-Drama");
    }
}
