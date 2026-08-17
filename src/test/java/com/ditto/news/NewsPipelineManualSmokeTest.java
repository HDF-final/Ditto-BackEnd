package com.ditto.news;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import com.ditto.news.config.CrawlerProperties;
import com.ditto.news.config.GeminiProperties;
import com.ditto.news.config.NewsFeedGenerationProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.outbound.collector.NewsKeywordFilter;
import com.ditto.news.outbound.collector.RssFeedClient;
import com.ditto.news.outbound.collector.RssNewsCollector;
import com.ditto.news.outbound.collector.RssXmlParser;
import com.ditto.news.outbound.crawler.DefaultNewsArticleCrawler;
import com.ditto.news.outbound.generator.GeminiNewsApiClient;
import com.ditto.news.outbound.generator.GeminiNewsFeedGenerator;
import com.ditto.news.outbound.selector.DefaultNewsArticleSelector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 실제 외부 RSS 피드 + Python Selenium 크롤러 서비스 + Google Gemini AI를 연동하는 수동 smoke test.
 *
 * <p>실행 방법:
 * <pre>
 *   ./gradlew test --tests "com.ditto.news.NewsPipelineManualSmokeTest" -DSMOKE_TEST=true --info
 * </pre>
 */
class NewsPipelineManualSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true",
            disabledReason = "수동 실행 전용 Smoke Test입니다. 실행하려면 SMOKE_TEST=true 환경변수를 전달하세요.")
    @DisplayName("실제 외부 RSS, Python 크롤러, Google Gemini를 연동하여 K-POP 뉴스 파이프라인 전체 흐름 및 생성 DTO를 콘솔에 출력한다")
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
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(60));

        RestClient crawlerRestClient = RestClient.builder()
                .baseUrl(crawlerProperties.getServiceUrl())
                .requestFactory(requestFactory)
                .build();
        DefaultNewsArticleCrawler crawler = new DefaultNewsArticleCrawler(crawlerRestClient, crawlerProperties, objectMapper);
        DefaultNewsArticleSelector selector = new DefaultNewsArticleSelector();

        GeminiProperties geminiProperties = new GeminiProperties();
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey == null || envKey.isBlank()) {
            envKey = System.getProperty("GEMINI_API_KEY", "AIzaSyAszhXMgpGq5EXXJHvwOeb9Vu1mmwW24vE");
        }
        geminiProperties.setApiKey(envKey);
        geminiProperties.setModel("gemini-2.5-flash");
        geminiProperties.setEnabled(true);

        RestClient geminiRestClient = RestClient.builder()
                .baseUrl(geminiProperties.getBaseUrl())
                .build();
        GeminiNewsApiClient geminiApiClient = new GeminiNewsApiClient(geminiRestClient, geminiProperties, objectMapper);
        GeminiNewsFeedGenerator generator = new GeminiNewsFeedGenerator(geminiApiClient);

        // 2. 1단계: RSS 기사 후보 수집 (K-POP 단일 토픽)
        System.out.println("\n[1단계] RSS 기사 후보 수집 (NewsArticleCollector.collect)");
        List<NewsArticleCandidate> candidates = collector.collect(topic);
        System.out.println(" -> 수집된 후보 기사 개수: " + candidates.size() + "건");
        System.out.println("-".repeat(80));

        int candidateIdx = 1;
        for (NewsArticleCandidate c : candidates) {
            System.out.printf(" [%d] 제목: %s%n", candidateIdx++, c.getTitle());
            System.out.printf("     URL: %s%n", c.getUrl());
            System.out.printf("     출처: %s | 발행일시: %s%n", c.getSource(), c.getPublishedAt());
        }

        // 3. 2단계: 크롤러 서비스를 통한 실제 기사 본문 크롤링
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

        // 5. 4단계: Gemini LLM 2차 뉴스피드 생성 (저작권 표절 방지 및 3줄 요약 DTO)
        System.out.println("=".repeat(80));
        System.out.println("[4단계] Google Gemini AI 2차 뉴스피드 콘텐츠 생성 (AiNewsFeedGenerator.generate)");
        GeneratedNewsFeed generatedFeed = generator.generate(selectedArticles, topic);

        if (generatedFeed != null) {
            System.out.println("\n🎉 [최종 생성된 GeneratedNewsFeed DTO 결과]");
            System.out.println(" - Title (대표 제목): " + generatedFeed.getTitle());
            System.out.println(" - Slug (URL 식별자): " + generatedFeed.getSlug());
            System.out.println(" - Representative Image URL: " + generatedFeed.getRepresentativeImageUrl());
            System.out.println(" - Keywords (태그): " + generatedFeed.getKeywords());
            System.out.println("\n ⭐️ Summaries (기사 요약 3줄):");
            if (generatedFeed.getSummaries() != null) {
                int sumIdx = 1;
                for (String summary : generatedFeed.getSummaries()) {
                    System.out.printf("    %d. %s%n", sumIdx++, summary);
                }
            } else {
                System.out.println("    (요약 없음)");
            }
            System.out.println("\n 📄 Body (재작성 본문):");
            System.out.println(generatedFeed.getBody());
        } else {
            System.out.println(" -> 뉴스피드 생성 결과가 null입니다.");
        }

        System.out.println("\n" + "=".repeat(80));
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
