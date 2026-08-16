package com.ditto.news.pipeline;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import com.ditto.news.config.NewsFeedGenerationProperties;
import com.ditto.news.crawler.CommonNewsCrawler;
import com.ditto.news.crawler.CrawlerProperties;
import com.ditto.news.crawler.DefaultNewsArticleCrawler;
import com.ditto.news.crawler.strategy.KoreaHeraldArticleParser;
import com.ditto.news.crawler.strategy.KoreaTimesArticleParser;
import com.ditto.news.crawler.strategy.NewsArticleParserStrategy;
import com.ditto.news.crawler.strategy.YonhapNewsArticleParser;
import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;
import com.ditto.news.selector.DefaultNewsArticleSelector;
import com.ditto.news.service.collector.NewsKeywordFilter;
import com.ditto.news.service.collector.RssFeedClient;
import com.ditto.news.service.collector.RssNewsCollector;
import com.ditto.news.service.collector.RssXmlParser;

/**
 * 외부 인터넷(실시간 RSS 피드 및 웹페이지)을 연동하는 수동 실행 전용 Smoke Test.
 * <p>
 * CI 및 일반 자동 테스트 스위트에서는 기본적으로 비활성화되며,
 * {@code -DsmokeTest=true} 옵션을 명시할 때만 실행됩니다.
 * </p>
 */
@Tag("manual")
class NewsPipelineManualSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true",
            disabledReason = "수동 실행 전용 Smoke Test입니다. 실행하려면 SMOKE_TEST=true 환경변수를 전달하세요.")
    @DisplayName("실제 외부 RSS 및 웹페이지를 연동하여 K-POP 뉴스 파이프라인 전체 흐름을 콘솔에 출력한다")
    void runRealNewsPipelineSmokeTest() {
        String topic = "K-POP";
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> DITTO AI 뉴스 파이프라인 실시간 외부 연동 수동 Smoke Test 시작 <<<");
        System.out.println(" - 대상 토픽: " + topic);
        System.out.println("=".repeat(80));

        // 1. 파이프라인 컴포넌트 초기화
        NewsFeedGenerationProperties properties = new NewsFeedGenerationProperties();
        properties.setTopics(List.of("K-POP"));

        RestClient restClient = RestClient.builder().build();
        RssFeedClient feedClient = new RssFeedClient(restClient);
        RssXmlParser xmlParser = new RssXmlParser();
        NewsKeywordFilter keywordFilter = new NewsKeywordFilter();
        RssNewsCollector collector = new RssNewsCollector(feedClient, xmlParser, keywordFilter, properties);

        CrawlerProperties crawlerProperties = new CrawlerProperties();
        CommonNewsCrawler commonNewsCrawler = new CommonNewsCrawler(crawlerProperties);
        List<NewsArticleParserStrategy> strategies = List.of(
                new KoreaHeraldArticleParser(),
                new KoreaTimesArticleParser(),
                new YonhapNewsArticleParser()
        );
        DefaultNewsArticleCrawler crawler = new DefaultNewsArticleCrawler(commonNewsCrawler, strategies);
        DefaultNewsArticleSelector selector = new DefaultNewsArticleSelector();

        // 2. 1단계: RSS 기사 후보 수집 (K-POP 단일 토픽)
        System.out.println("\n[1단계] RSS 기사 후보 수집 (NewsArticleCollector.collect)");
        List<NewsArticleCandidate> candidates = collector.collect(topic);
        System.out.println(" -> K-POP 후보 기사 수집 결과: " + candidates.size() + "건");
        System.out.println("-".repeat(80));

        int candidateIdx = 1;
        for (NewsArticleCandidate c : candidates) {
            System.out.printf(" [%d] 제목: %s%n", candidateIdx++, c.getTitle());
            System.out.printf("     URL: %s%n", c.getUrl());
            System.out.printf("     언론사: %s | 발행일시: %s | 매칭키워드: %s%n",
                    c.getSource(), c.getPublishedAt(), c.getMatchedKeyword());
        }

        if (candidates.isEmpty()) {
            System.out.println(" ! 알림: 실시간 RSS 피드에 현재 K-POP 관련 기사가 0건입니다.");
            System.out.println("=".repeat(80));
            return;
        }

        // 3. 2단계: 실제 웹페이지 기사 본문 크롤링
        System.out.println("\n" + "=".repeat(80));
        System.out.println("[2단계] 기사 상세 본문 크롤링 (NewsArticleCrawler.crawlAll)");
        List<CrawledNewsArticle> crawledArticles = crawler.crawlAll(candidates);
        System.out.println(" -> 크롤링 성공 기사 개수: " + crawledArticles.size() + "건");
        System.out.println("-".repeat(80));

        int crawledIdx = 1;
        for (CrawledNewsArticle a : crawledArticles) {
            System.out.printf(" [%d] 제목: %s%n", crawledIdx++, a.getTitle());
            System.out.printf("     URL: %s%n", a.getUrl());
            System.out.printf("     언론사: %s | 발행일시: %s | 대표이미지: %s%n",
                    a.getSource(), a.getPublishedAt(), a.getImageUrl() != null ? a.getImageUrl() : "(이미지 없음)");
            System.out.println("     본문 요약 (앞 250자):");
            System.out.println("     \"" + truncateText(a.getBody(), 250) + "\"");
        }

        // 4. 3단계: K-POP 관련성 필터링 및 중복 제거 / 상위 N개 선별
        System.out.println("\n" + "=".repeat(80));
        System.out.println("[3단계] K-POP 기사 선별 및 중복 제거 (NewsArticleSelector.selectRelevantArticles)");
        List<CrawledNewsArticle> selectedArticles = selector.selectRelevantArticles(crawledArticles, List.of(topic));
        System.out.println(" -> 최종 선별된 K-POP 기사 개수: " + selectedArticles.size() + "건 (최대 5건)");
        System.out.println("-".repeat(80));

        int selectedIdx = 1;
        for (CrawledNewsArticle s : selectedArticles) {
            System.out.printf(" ★ [선별 기사 %d] 제목: %s%n", selectedIdx++, s.getTitle());
            System.out.printf("   - URL: %s%n", s.getUrl());
            System.out.printf("   - 출처: %s%n", s.getSource());
            System.out.printf("   - 발행일시: %s%n", s.getPublishedAt());
            System.out.printf("   - 대표이미지: %s%n", s.getImageUrl() != null ? s.getImageUrl() : "(없음)");
            System.out.println("   - 본문 요약:");
            System.out.println("     \"" + truncateText(s.getBody(), 200) + "\"");
            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.println(" >>> DITTO AI 뉴스 파이프라인 실시간 외부 연동 수동 Smoke Test 완료 <<<");
        System.out.println("=".repeat(80) + "\n");
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "(본문 없음)";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "...";
    }
}
