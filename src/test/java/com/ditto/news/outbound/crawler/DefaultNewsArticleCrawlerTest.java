package com.ditto.news.outbound.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.config.CrawlerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DefaultNewsArticleCrawlerTest {

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private CrawlerProperties properties;
    private ObjectMapper objectMapper;
    private DefaultNewsArticleCrawler crawler;

    @BeforeEach
    void setUp() {
        properties = CrawlerProperties.builder()
                .serviceUrl("http://localhost:8000")
                .crawlPath("/crawl")
                .connectTimeoutSeconds(5)
                .readTimeoutSeconds(10)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getServiceUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        crawler = new DefaultNewsArticleCrawler(restClient, properties, objectMapper);
    }

    @Test
    @DisplayName("Python 크롤러 서비스에 요청을 전송하고 정상 응답을 CrawledNewsArticle로 매핑한다")
    void crawlsArticleSuccessfully() {
        String url = "https://www.koreaherald.com/view.php?ud=202608160001";
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("K-POP Single Release")
                .url(url)
                .source("The Korea Herald")
                .build();

        String jsonResponse = """
                {
                    "title": "New Jeans Summer Comeback",
                    "body": "New Jeans dropped their highly anticipated summer single.\\n\\nThe music video surpassed 10 million views.",
                    "url": "https://www.koreaherald.com/view.php?ud=202608160001",
                    "source": "The Korea Herald",
                    "published_at": "2026-08-16T12:00:00+09:00",
                    "image_url": "https://img.koreaherald.com/photo.jpg"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/crawl"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        CrawledNewsArticle result = crawler.crawl(candidate);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Jeans Summer Comeback");
        assertThat(result.getBody()).contains("New Jeans dropped their highly anticipated");
        assertThat(result.getUrl()).isEqualTo(url);
        assertThat(result.getSource()).isEqualTo("The Korea Herald");
        assertThat(result.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 16, 12, 0, 0));
        assertThat(result.getImageUrl()).isEqualTo("https://img.koreaherald.com/photo.jpg");

        mockServer.verify();
    }

    @Test
    @DisplayName("candidate 또는 URL이 null/blank인 경우 INVALID_INPUT_VALUE 예외를 던진다")
    void validatesCandidateAndUrl() {
        assertThatThrownBy(() -> crawler.crawl(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        NewsArticleCandidate blankUrlCandidate = NewsArticleCandidate.builder()
                .title("Blank URL")
                .url("   ")
                .build();
        assertThatThrownBy(() -> crawler.crawl(blankUrlCandidate))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("Python 크롤러 서비스가 5xx 에러 또는 응답 실패 시 NEWS_CRAWLING_FAILED 예외를 던진다")
    void throwsExceptionOnServiceFailure() {
        String url = "https://www.yna.co.kr/view/AKR20260816000100005";
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("Yonhap News")
                .url(url)
                .build();

        mockServer.expect(requestTo("http://localhost:8000/crawl"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> crawler.crawl(candidate))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_CRAWLING_FAILED));

        mockServer.verify();
    }

    @Test
    @DisplayName("crawlAll은 개별 기사 크롤링 실패 시에도 건너뛰고 성공한 기사들을 반환한다 (Fault Tolerance)")
    void crawlAllHandlesIndividualFailures() {
        NewsArticleCandidate goodCandidate = NewsArticleCandidate.builder()
                .title("Good")
                .url("https://www.koreaherald.com/view.php?ud=1")
                .build();

        NewsArticleCandidate badCandidate = NewsArticleCandidate.builder()
                .title("Bad")
                .url("https://www.koreatimes.co.kr/www/art/1.html")
                .build();

        String jsonResponse = """
                {
                    "title": "Good Article",
                    "body": "Valid article body text.",
                    "url": "https://www.koreaherald.com/view.php?ud=1"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/crawl"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8000/crawl"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        List<CrawledNewsArticle> results = crawler.crawlAll(List.of(goodCandidate, badCandidate));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Good Article");
        mockServer.verify();
    }

    @Test
    @DisplayName("crawlAll에 빈 목록이나 null이 전달되면 빈 목록을 반환한다")
    void crawlAllHandlesEmpty() {
        assertThat(crawler.crawlAll(null)).isEmpty();
        assertThat(crawler.crawlAll(Collections.emptyList())).isEmpty();
    }
}
