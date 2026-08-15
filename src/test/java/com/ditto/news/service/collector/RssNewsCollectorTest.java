package com.ditto.news.service.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.ditto.news.config.NewsFeedGenerationProperties;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

@ExtendWith(MockitoExtension.class)
class RssNewsCollectorTest {

    @Mock
    private RssFeedClient feedClient;

    private RssXmlParser xmlParser;
    private NewsKeywordFilter keywordFilter;
    private NewsFeedGenerationProperties properties;
    private RssNewsCollector collector;

    private static final List<String> DEFAULT_TOPICS = List.of(
            "K-POP",
            "K-뷰티",
            "K-패션",
            "한국 팝업스토어",
            "서울 핫플"
    );

    @BeforeEach
    void setUp() {
        xmlParser = new RssXmlParser();
        keywordFilter = new NewsKeywordFilter();
        properties = new NewsFeedGenerationProperties();
        properties.setTopics(DEFAULT_TOPICS);

        collector = new RssNewsCollector(feedClient, xmlParser, keywordFilter, properties);
    }

    @Test
    @DisplayName("RSS 2.0 XML item으로부터 title, url, source, publishedAt, description이 정확히 추출된다")
    void extractsCandidateFromRss2Xml() {
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <channel>
                    <title>The Korea Herald Culture</title>
                    <link>http://www.koreaherald.com</link>
                    <item>
                      <title><![CDATA[New Jeans & IVE lead global K-POP renaissance]]></title>
                      <link>https://www.koreaherald.com/view.php?ud=202608160001</link>
                      <description><![CDATA[K-POP artists continue to dominate international streaming platforms &amp; Billboard charts.]]></description>
                      <pubDate>Fri, 15 Aug 2026 12:00:00 GMT</pubDate>
                      <source url="http://www.koreaherald.com">The Korea Herald</source>
                    </item>
                  </channel>
                </rss>
                """;

        List<NewsArticleCandidate> candidates = collector.collectFromXml(rssXml, "Fallback Source", DEFAULT_TOPICS);

        assertThat(candidates).hasSize(1);
        NewsArticleCandidate candidate = candidates.get(0);

        assertThat(candidate.getTitle()).isEqualTo("New Jeans & IVE lead global K-POP renaissance");
        assertThat(candidate.getUrl()).isEqualTo("https://www.koreaherald.com/view.php?ud=202608160001");
        assertThat(candidate.getSource()).isEqualTo("The Korea Herald");
        assertThat(candidate.getDescription()).isEqualTo("K-POP artists continue to dominate international streaming platforms & Billboard charts.");
        assertThat(candidate.getMatchedKeyword()).isEqualTo("K-POP");

        // 12:00:00 GMT -> Asia/Seoul (UTC+9) 21:00:00
        assertThat(candidate.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 15, 21, 0, 0));
    }

    @Test
    @DisplayName("Atom XML entry로부터 candidate가 정확히 변환된다")
    void extractsCandidateFromAtomXml() {
        String atomXml = """
                <?xml version="1.0" encoding="utf-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Korea Lifestyle Feed</title>
                  <entry>
                    <title>2026 성수동 한국 팝업스토어 베스트 투어 가이드</title>
                    <link href="https://example.com/popup/2026" />
                    <summary>성수동 일대에서 펼쳐지는 인기 브랜드 팝업스토어 소개</summary>
                    <published>2026-08-16T10:00:00+09:00</published>
                    <author>
                      <name>Lifestyle Korea</name>
                    </author>
                  </entry>
                </feed>
                """;

        List<NewsArticleCandidate> candidates = collector.collectFromXml(atomXml, "Default Feed", DEFAULT_TOPICS);

        assertThat(candidates).hasSize(1);
        NewsArticleCandidate candidate = candidates.get(0);

        assertThat(candidate.getTitle()).isEqualTo("2026 성수동 한국 팝업스토어 베스트 투어 가이드");
        assertThat(candidate.getUrl()).isEqualTo("https://example.com/popup/2026");
        assertThat(candidate.getSource()).isEqualTo("Lifestyle Korea");
        assertThat(candidate.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 16, 10, 0, 0));
        assertThat(candidate.getMatchedKeyword()).isEqualTo("한국 팝업스토어");
    }

    @Test
    @DisplayName("동일한 URL을 가진 기사 후보는 최초 수집된 1건만 유지되고 중복이 제거된다")
    void deduplicatesCandidatesByUrl() {
        String feedXml1 = """
                <rss version="2.0">
                  <channel>
                    <title>Feed 1</title>
                    <item>
                      <title>K-패션 글로벌 팝업 성료</title>
                      <link>https://example.com/news/kfashion-100</link>
                      <pubDate>Fri, 15 Aug 2026 10:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        String feedXml2 = """
                <rss version="2.0">
                  <channel>
                    <title>Feed 2</title>
                    <item>
                      <title>[재배포] K-패션 글로벌 팝업 성료</title>
                      <link>https://example.com/news/kfashion-100</link>
                      <pubDate>Fri, 15 Aug 2026 11:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        List<NewsArticleCandidate> results = collector.collectFromXmlList(
                List.of(feedXml1, feedXml2), "Default Source", DEFAULT_TOPICS);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUrl()).isEqualTo("https://example.com/news/kfashion-100");
        assertThat(results.get(0).getTitle()).isEqualTo("K-패션 글로벌 팝업 성료");
    }

    @Test
    @DisplayName("빈 피드(아이템 없음 또는 빈 태그)는 예외 없이 빈 리스트를 반환한다")
    void handlesEmptyFeedGracefully() {
        String emptyRss = """
                <rss version="2.0">
                  <channel>
                    <title>Empty Channel</title>
                  </channel>
                </rss>
                """;

        List<NewsArticleCandidate> results = collector.collectFromXml(emptyRss, "Source", DEFAULT_TOPICS);
        assertThat(results).isEmpty();

        List<NewsArticleCandidate> nullResults = collector.collectFromXml(null, "Source", DEFAULT_TOPICS);
        assertThat(nullResults).isEmpty();

        List<NewsArticleCandidate> blankResults = collector.collectFromXml("   ", "Source", DEFAULT_TOPICS);
        assertThat(blankResults).isEmpty();
    }

    @Test
    @DisplayName("구문 오류가 있는 XML이나 필수 필드가 누락된 malformed item은 안전하게 건너뛴다")
    void handlesMalformedXmlAndItemsSafely() {
        // 1. 깨진 XML 구문
        String corruptXml = "<rss><channel><item><title>미완성 태그";
        List<NewsArticleCandidate> corruptResults = collector.collectFromXml(corruptXml, "Source", DEFAULT_TOPICS);
        assertThat(corruptResults).isEmpty();

        // 2. 제목 누락 / 링크 누락 / 잘못된 프로토콜 / 잘못된 날짜
        String mixedXml = """
                <rss version="2.0">
                  <channel>
                    <title>Mixed Quality Feed</title>
                    <!-- 필수 필드(link) 누락: 제외 -->
                    <item>
                      <title>K-POP 콘서트 일정 안내</title>
                    </item>
                    <!-- 필수 필드(title) 누락: 제외 -->
                    <item>
                      <link>https://example.com/missing-title</link>
                    </item>
                    <!-- 잘못된 URL 프로토콜: 제외 -->
                    <item>
                      <title>K-POP 페스티벌</title>
                      <link>javascript:alert(1)</link>
                    </item>
                    <!-- 날짜 형식 파싱 불능: publishedAt=null 로 안전 파싱 후 키워드 매칭 -->
                    <item>
                      <title>서울 핫플 망원동 맛집 투어</title>
                      <link>https://example.com/seoul-hotplace</link>
                      <pubDate>not-a-valid-date</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        List<NewsArticleCandidate> mixedResults = collector.collectFromXml(mixedXml, "Channel Source", DEFAULT_TOPICS);

        assertThat(mixedResults).hasSize(1);
        assertThat(mixedResults.get(0).getTitle()).isEqualTo("서울 핫플 망원동 맛집 투어");
        assertThat(mixedResults.get(0).getUrl()).isEqualTo("https://example.com/seoul-hotplace");
        assertThat(mixedResults.get(0).getPublishedAt()).isNull();
        assertThat(mixedResults.get(0).getMatchedKeyword()).isEqualTo("서울 핫플");
    }

    @Test
    @DisplayName("한 피드 요청이 실패(네트워크 오류/예외)하더라도 다른 피드의 수집은 정상 수행된다")
    void continuesCollectionWhenOneFeedFails() {
        RssFeedSource goodFeed = RssFeedSource.builder()
                .name("Good Feed")
                .url("https://good.example.com/rss.xml")
                .build();

        RssFeedSource badFeed = RssFeedSource.builder()
                .name("Bad Feed")
                .url("https://bad.example.com/rss.xml")
                .build();

        String goodFeedXml = """
                <rss version="2.0">
                  <channel>
                    <title>Good Feed</title>
                    <item>
                      <title>2026 K-뷰티 스킨케어 신제품 발표</title>
                      <link>https://good.example.com/news/1</link>
                      <pubDate>Sat, 16 Aug 2026 01:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """;

        given(feedClient.fetchXml(badFeed.getUrl())).willThrow(new RestClientException("Connection timed out"));
        given(feedClient.fetchXml(goodFeed.getUrl())).willReturn(goodFeedXml);

        List<NewsArticleCandidate> results = collector.collectCandidates(
                List.of(badFeed, goodFeed), DEFAULT_TOPICS);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("2026 K-뷰티 스킨케어 신제품 발표");
        assertThat(results.get(0).getUrl()).isEqualTo("https://good.example.com/news/1");
        assertThat(results.get(0).getMatchedKeyword()).isEqualTo("K-뷰티");
    }

    @Test
    @DisplayName("1차 키워드 필터는 K-컬처 주제에 부합하지 않는 일반 기사를 걸러낸다")
    void filtersOutNonKCultureArticles() {
        String mixedTopicsXml = """
                <rss version="2.0">
                  <channel>
                    <title>General Daily News</title>
                    <item>
                      <title>K-POP 글로벌 음원 스트리밍 1위 기록</title>
                      <link>https://example.com/news/1</link>
                    </item>
                    <item>
                      <title>국제 유가 상승으로 항공권 유류할증료 인상</title>
                      <link>https://example.com/news/2</link>
                    </item>
                    <item>
                      <title>미국 증시 다우존스 지수 상승 마감</title>
                      <link>https://example.com/news/3</link>
                    </item>
                    <item>
                      <title>K-패션 디자이너 브랜드 파리 패션위크 참가</title>
                      <link>https://example.com/news/4</link>
                    </item>
                  </channel>
                </rss>
                """;

        List<NewsArticleCandidate> results = collector.collectFromXml(mixedTopicsXml, "General", DEFAULT_TOPICS);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(NewsArticleCandidate::getTitle)
                .containsExactly(
                        "K-POP 글로벌 음원 스트리밍 1위 기록",
                        "K-패션 디자이너 브랜드 파리 패션위크 참가"
                );
    }

    @Test
    @DisplayName("NewsArticleCollector 인터페이스의 collect(keyword) 단일 토픽 수집이 정상 작동한다")
    void collectsBySingleTopic() {
        String feedXml = """
                <rss version="2.0">
                  <channel>
                    <title>Herald</title>
                    <item>
                      <title>K-POP 콘서트 예매 오픈</title>
                      <link>https://example.com/kpop-1</link>
                    </item>
                    <item>
                      <title>K-뷰티 팝업스토어</title>
                      <link>https://example.com/kbeauty-1</link>
                    </item>
                  </channel>
                </rss>
                """;

        given(feedClient.fetchXml(org.mockito.ArgumentMatchers.anyString())).willReturn(feedXml);

        List<NewsArticleCandidate> kpopResults = collector.collect("K-POP");

        assertThat(kpopResults).hasSize(1);
        assertThat(kpopResults.get(0).getTitle()).isEqualTo("K-POP 콘서트 예매 오픈");
        assertThat(kpopResults.get(0).getMatchedKeyword()).isEqualTo("K-POP");

        // null or blank keyword returns empty list
        assertThat(collector.collect(null)).isEmpty();
        assertThat(collector.collect("   ")).isEmpty();
    }

    @Test
    @DisplayName("NewsArticleCollector 인터페이스의 collectAll(keywords) 다중 토픽 일괄 수집이 정상 작동한다")
    void collectsByMultipleTopics() {
        String feedXml = """
                <rss version="2.0">
                  <channel>
                    <title>Herald</title>
                    <item>
                      <title>K-POP 콘서트 예매 오픈</title>
                      <link>https://example.com/kpop-1</link>
                    </item>
                    <item>
                      <title>K-뷰티 팝업스토어 오픈</title>
                      <link>https://example.com/kbeauty-1</link>
                    </item>
                    <item>
                      <title>일반 경제 뉴스</title>
                      <link>https://example.com/econ-1</link>
                    </item>
                  </channel>
                </rss>
                """;

        given(feedClient.fetchXml(org.mockito.ArgumentMatchers.anyString())).willReturn(feedXml);

        List<NewsArticleCandidate> results = collector.collectAll(List.of("K-POP", "K-뷰티"));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(NewsArticleCandidate::getMatchedKeyword)
                .containsExactlyInAnyOrder("K-POP", "K-뷰티");

        // null or empty list returns empty list
        assertThat(collector.collectAll(null)).isEmpty();
        assertThat(collector.collectAll(List.of())).isEmpty();
    }
}
