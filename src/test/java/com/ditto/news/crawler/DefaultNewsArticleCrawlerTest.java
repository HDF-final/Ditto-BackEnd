package com.ditto.news.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.crawler.strategy.KoreaHeraldArticleParser;
import com.ditto.news.crawler.strategy.KoreaTimesArticleParser;
import com.ditto.news.crawler.strategy.NewsArticleParserStrategy;
import com.ditto.news.crawler.strategy.YonhapNewsArticleParser;
import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

@ExtendWith(MockitoExtension.class)
class DefaultNewsArticleCrawlerTest {

    @Mock
    private CommonNewsCrawler commonNewsCrawler;

    private KoreaHeraldArticleParser koreaHeraldParser;
    private KoreaTimesArticleParser koreaTimesParser;
    private YonhapNewsArticleParser yonhapParser;

    private DefaultNewsArticleCrawler crawler;

    @BeforeEach
    void setUp() {
        koreaHeraldParser = new KoreaHeraldArticleParser();
        koreaTimesParser = new KoreaTimesArticleParser();
        yonhapParser = new YonhapNewsArticleParser();

        List<NewsArticleParserStrategy> strategies = List.of(
                koreaHeraldParser, koreaTimesParser, yonhapParser
        );
        crawler = new DefaultNewsArticleCrawler(commonNewsCrawler, strategies);
    }

    @Test
    @DisplayName("후보 URL에 맞는 Strategy를 탐색하고 CommonNewsCrawler를 호출하여 기사를 정상 크롤링한다")
    void crawlsArticleSuccessfully() {
        String url = "https://www.koreaherald.com/view.php?ud=202608160001";
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("K-POP New Single Release")
                .url(url)
                .source("The Korea Herald")
                .build();

        String html = """
                <html>
                <body>
                  <h1 class="view_tit">New Jeans Summer Comeback</h1>
                  <div id="articleText">
                    <p>New Jeans dropped their highly anticipated summer single.</p>
                    <p>The music video surpassed 10 million views in 6 hours.</p>
                  </div>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html, url);
        given(commonNewsCrawler.fetchDocument(url)).willReturn(doc);

        CrawledNewsArticle result = crawler.crawl(candidate);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Jeans Summer Comeback");
        assertThat(result.getBody()).contains("New Jeans dropped their highly anticipated");
        assertThat(result.getUrl()).isEqualTo(url);
        assertThat(result.getSource()).isEqualTo("The Korea Herald");

        verify(commonNewsCrawler).fetchDocument(url);
    }

    @Test
    @DisplayName("지원하지 않는 사이트 URL은 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsExceptionForUnsupportedDomain() {
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("Unsupported")
                .url("https://www.unsupported-news.com/article/1")
                .build();

        assertThatThrownBy(() -> crawler.crawl(candidate))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
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

        String html = """
                <html>
                <body>
                  <h1 class="view_tit">Good Article Title</h1>
                  <div id="articleText"><p>Valid article body paragraph.</p></div>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html, goodCandidate.getUrl());
        given(commonNewsCrawler.fetchDocument(goodCandidate.getUrl())).willReturn(doc);
        given(commonNewsCrawler.fetchDocument(badCandidate.getUrl()))
                .willThrow(new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED));

        List<CrawledNewsArticle> results = crawler.crawlAll(List.of(goodCandidate, badCandidate));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Good Article Title");
    }

    @Test
    @DisplayName("crawlAll에 빈 목록이나 null이 전달되면 빈 목록을 반환한다")
    void crawlAllHandlesEmpty() {
        assertThat(crawler.crawlAll(null)).isEmpty();
        assertThat(crawler.crawlAll(Collections.emptyList())).isEmpty();
    }
}
