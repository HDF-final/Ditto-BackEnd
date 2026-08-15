package com.ditto.news.crawler.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

class NewsArticleParserStrategyTest {

    private KoreaHeraldArticleParser koreaHeraldParser;
    private KoreaTimesArticleParser koreaTimesParser;
    private YonhapNewsArticleParser yonhapParser;

    @BeforeEach
    void setUp() {
        koreaHeraldParser = new KoreaHeraldArticleParser();
        koreaTimesParser = new KoreaTimesArticleParser();
        yonhapParser = new YonhapNewsArticleParser();
    }

    @Nested
    @DisplayName("supports() URL 및 도메인 검증 테스트")
    class SupportsTest {

        @Test
        @DisplayName("정상적인 지원 사이트 URL 및 서브도메인을 정확히 식별한다")
        void supportsValidUrls() {
            assertThat(koreaHeraldParser.supports("http://www.koreaherald.com/view.php?ud=202608160001")).isTrue();
            assertThat(koreaHeraldParser.supports("https://koreaherald.com/article/123")).isTrue();

            assertThat(koreaTimesParser.supports("https://www.koreatimes.co.kr/www/culture/2026/08/123.html")).isTrue();
            assertThat(koreaTimesParser.supports("http://koreatimes.co.kr/news/1")).isTrue();

            assertThat(yonhapParser.supports("https://www.yna.co.kr/view/AKR20260816000100005")).isTrue();
            assertThat(yonhapParser.supports("https://en.yna.co.kr/view/AEN20260816000100315")).isTrue();
            assertThat(yonhapParser.supports("https://yna.co.kr/view/123")).isTrue();
        }

        @Test
        @DisplayName("타사 도메인 및 미지원 사이트는 false를 반환한다")
        void rejectsOtherDomains() {
            assertThat(koreaHeraldParser.supports("https://www.koreatimes.co.kr/article")).isFalse();
            assertThat(koreaTimesParser.supports("https://www.yna.co.kr/view/1")).isFalse();
            assertThat(yonhapParser.supports("https://www.koreaherald.com/1")).isFalse();
            assertThat(yonhapParser.supports("https://www.bbc.com/news/123")).isFalse();
        }

        @Test
        @DisplayName("null, 빈 문자열, 비HTTP(S) 프로토콜은 예외 없이 false를 반환한다")
        void rejectsInvalidProtocolsAndBlanks() {
            assertThat(koreaHeraldParser.supports(null)).isFalse();
            assertThat(koreaHeraldParser.supports("")).isFalse();
            assertThat(koreaHeraldParser.supports("   ")).isFalse();
            assertThat(koreaHeraldParser.supports("ftp://www.koreaherald.com/1")).isFalse();
            assertThat(koreaHeraldParser.supports("javascript:alert(1)")).isFalse();
        }

        @Test
        @DisplayName("쿼리스트링 위장, 서브도메인 위장, 접두어 유사 도메인 등 위장 URL을 완벽히 차단한다")
        void rejectsSpoofedUrls() {
            // 1. 쿼리 파라미터로 위장한 경우
            assertThat(koreaHeraldParser.supports("https://evil.com/?target=https://koreaherald.com")).isFalse();
            assertThat(koreaTimesParser.supports("https://attacker.org/news?site=koreatimes.co.kr")).isFalse();

            // 2. 공격자 도메인의 서브도메인으로 위장한 경우
            assertThat(koreaHeraldParser.supports("https://koreaherald.com.evil.com/phishing")).isFalse();
            assertThat(yonhapParser.supports("https://yna.co.kr.fake-news.net/view")).isFalse();

            // 3. 점(.) 경계 없는 유사 도메인
            assertThat(koreaHeraldParser.supports("https://fakekoreaherald.com/view")).isFalse();
            assertThat(koreaTimesParser.supports("https://notkoreatimes.co.kr/view")).isFalse();
            assertThat(yonhapParser.supports("https://myyna.co.kr/view")).isFalse();
        }
    }

    @Nested
    @DisplayName("The Korea Herald 기사 상세 파싱 테스트")
    class KoreaHeraldParsingTest {

        @Test
        @DisplayName("Korea Herald HTML Fixture로부터 제목, 다중 문단 본문, 발행일, 이미지 등을 정확히 추출한다")
        void parsesKoreaHeraldArticle() {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <title>K-POP Global Record - The Korea Herald</title>
                      <meta property="og:title" content="OG Title K-POP" />
                      <meta property="og:image" content="https://img.koreaherald.com/sample.jpg" />
                      <meta property="article:published_time" content="2026-08-16T15:30:00+09:00" />
                      <link rel="canonical" href="https://www.koreaherald.com/view.php?ud=202608160001" />
                    </head>
                    <body>
                      <h1 class="view_tit">New Jeans Shatters Global Streaming Milestone</h1>
                      <div class="view_tit_by">
                        <span>Published : Aug 16, 2026 - 15:30</span>
                      </div>
                      <div id="articleText">
                        <script>var ad = 1;</script>
                        <div class="sns_share">Share Button</div>
                        <p>K-pop sensation New Jeans has achieved another unprecedented milestone on global music platforms.</p>
                        <p>   The five-member group topped multiple international charts simultaneously.   </p>
                        <div class="article-ad">Banner Ad</div>
                        <p>Fans across the globe celebrated the release of their latest summer project.</p>
                        <p class="copyright">(c) All rights reserved. The Korea Herald</p>
                      </div>
                    </body>
                    </html>
                    """;

            Document doc = Jsoup.parse(html, "https://www.koreaherald.com");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Candidate Title")
                    .url("https://www.koreaherald.com/view.php?ud=202608160001")
                    .source("The Korea Herald")
                    .build();

            CrawledNewsArticle article = koreaHeraldParser.parse(candidate, doc);

            assertThat(article.getTitle()).isEqualTo("New Jeans Shatters Global Streaming Milestone");
            assertThat(article.getUrl()).isEqualTo("https://www.koreaherald.com/view.php?ud=202608160001");
            assertThat(article.getSource()).isEqualTo("The Korea Herald");
            assertThat(article.getImageUrl()).isEqualTo("https://img.koreaherald.com/sample.jpg");
            assertThat(article.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 16, 15, 30, 0));

            // 문단 3개가 \n\n으로 연결되고 광고, 스크립트, 저작권 문구가 정제되었는지 검증
            String[] paragraphs = article.getBody().split("\n\n");
            assertThat(paragraphs).hasSize(3);
            assertThat(paragraphs[0]).isEqualTo("K-pop sensation New Jeans has achieved another unprecedented milestone on global music platforms.");
            assertThat(paragraphs[1]).isEqualTo("The five-member group topped multiple international charts simultaneously.");
            assertThat(paragraphs[2]).isEqualTo("Fans across the globe celebrated the release of their latest summer project.");
        }
    }

    @Nested
    @DisplayName("The Korea Times 기사 상세 파싱 테스트")
    class KoreaTimesParsingTest {

        @Test
        @DisplayName("Korea Times HTML Fixture로부터 제목, 다중 문단 본문, 발행일 등을 정확히 추출한다")
        void parsesKoreaTimesArticle() {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta property="og:image" content="https://img.koreatimes.co.kr/photo.jpg" />
                      <link rel="canonical" href="https://www.koreatimes.co.kr/www/art/2026/08/sample.html" />
                    </head>
                    <body>
                      <div class="view_head">
                        <h1>Seoul Fashion Week Showcases Next-Gen K-Style</h1>
                      </div>
                      <div class="view_head_info">
                        <span class="date">2026-08-16 11:00:00</span>
                      </div>
                      <div id="article-body">
                        <style>.ad {display:none;}</style>
                        <p>Leading designers gathered at DDP to unveil innovative spring and summer collections.</p>
                        <p></p>
                        <p>Sustainable fabrics and modern silhouettes took center stage throughout the runway shows.</p>
                      </div>
                    </body>
                    </html>
                    """;

            Document doc = Jsoup.parse(html, "https://www.koreatimes.co.kr");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Candidate Fallback Title")
                    .url("https://www.koreatimes.co.kr/www/art/2026/08/sample.html")
                    .build();

            CrawledNewsArticle article = koreaTimesParser.parse(candidate, doc);

            assertThat(article.getTitle()).isEqualTo("Seoul Fashion Week Showcases Next-Gen K-Style");
            assertThat(article.getSource()).isEqualTo("The Korea Times");
            assertThat(article.getImageUrl()).isEqualTo("https://img.koreatimes.co.kr/photo.jpg");
            assertThat(article.getPublishedAt()).isEqualTo(LocalDateTime.of(2026, 8, 16, 11, 0, 0));

            String[] paragraphs = article.getBody().split("\n\n");
            assertThat(paragraphs).hasSize(2);
            assertThat(paragraphs[0]).contains("Leading designers gathered");
            assertThat(paragraphs[1]).contains("Sustainable fabrics");
        }
    }

    @Nested
    @DisplayName("Yonhap News 기사 상세 파싱 테스트")
    class YonhapParsingTest {

        @Test
        @DisplayName("Yonhap News HTML Fixture로부터 제목, 본문, 발행일을 추출한다")
        void parsesYonhapArticle() {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta property="og:title" content="K-뷰티 팝업스토어 성수동에 오픈" />
                      <meta property="og:image" content="https://img.yna.co.kr/sample.png" />
                    </head>
                    <body>
                      <h1 class="tit">K-뷰티 신제품 팝업스토어 성수동 상륙</h1>
                      <div class="info-box">
                        <p class="update-time">송고시간 2026-08-16 09:15</p>
                      </div>
                      <article class="story-news">
                        <p>글로벌 K-뷰티 브랜드가 성수동 연무장길에 플래그십 팝업을 개관했다.</p>
                        <p>이번 행사에는 사전 예약에만 3만 명이 몰리며 뜨거운 인기를 입증했다.</p>
                        <p>reporter@yna.co.kr</p>
                      </article>
                    </body>
                    </html>
                    """;

            Document doc = Jsoup.parse(html, "https://www.yna.co.kr");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Candidate Title")
                    .url("https://www.yna.co.kr/view/AKR20260816000100005")
                    .source("연합뉴스")
                    .build();

            CrawledNewsArticle article = yonhapParser.parse(candidate, doc);

            assertThat(article.getTitle()).isEqualTo("K-뷰티 신제품 팝업스토어 성수동 상륙");
            assertThat(article.getSource()).isEqualTo("연합뉴스"); // candidate source 우선
            assertThat(article.getImageUrl()).isEqualTo("https://img.yna.co.kr/sample.png");

            String[] paragraphs = article.getBody().split("\n\n");
            assertThat(paragraphs).hasSize(2); // 이메일 행 제외
            assertThat(paragraphs[0]).contains("성수동 연무장길");
            assertThat(paragraphs[1]).contains("사전 예약에만 3만 명");
        }
    }

    @Nested
    @DisplayName("Fallback 및 예외 안전 처리 테스트")
    class FallbackAndErrorTest {

        @Test
        @DisplayName("HTML 내 제목 selector가 없으면 og:title 또는 candidate title로 fallback한다")
        void titleFallbackTest() {
            String html = """
                    <html>
                    <head><title>Document Title</title></head>
                    <body><div id="articleText"><p>본문 내용입니다.</p></div></body>
                    </html>
                    """;
            Document doc = Jsoup.parse(html, "https://www.koreaherald.com");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Candidate Fallback Title")
                    .url("https://www.koreaherald.com/1")
                    .build();

            CrawledNewsArticle article = koreaHeraldParser.parse(candidate, doc);
            assertThat(article.getTitle()).isEqualTo("Document Title");
        }

        @Test
        @DisplayName("HTML 내 발행일이 없으면 Candidate의 publishedAt을 유지한다")
        void publishedAtCandidateFallback() {
            String html = """
                    <html>
                    <body>
                      <h1 class="view_tit">Title</h1>
                      <div id="articleText"><p>Body text here.</p></div>
                    </body>
                    </html>
                    """;
            LocalDateTime candidateDate = LocalDateTime.of(2026, 8, 14, 10, 0);
            Document doc = Jsoup.parse(html, "https://www.koreaherald.com");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Title")
                    .url("https://www.koreaherald.com/1")
                    .publishedAt(candidateDate)
                    .build();

            CrawledNewsArticle article = koreaHeraldParser.parse(candidate, doc);
            assertThat(article.getPublishedAt()).isEqualTo(candidateDate);
        }

        @Test
        @DisplayName("og:image가 없거나 비정상 URL인 경우 imageUrl은 null이 된다")
        void handlesMissingOrInvalidImage() {
            String html = """
                    <html>
                    <head><meta property="og:image" content="data:image/png;base64,invalid" /></head>
                    <body>
                      <h1 class="view_tit">Title</h1>
                      <div id="articleText"><p>Body text here.</p></div>
                    </body>
                    </html>
                    """;
            Document doc = Jsoup.parse(html, "https://www.koreaherald.com");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Title")
                    .url("https://www.koreaherald.com/1")
                    .build();

            CrawledNewsArticle article = koreaHeraldParser.parse(candidate, doc);
            assertThat(article.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("본문 컨테이너가 비어있거나 유효한 문단이 하나도 없으면 NEWS_CRAWLING_FAILED 예외를 던진다")
        void throwsExceptionWhenBodyIsEmpty() {
            String emptyBodyHtml = """
                    <html>
                    <body>
                      <h1 class="view_tit">Title</h1>
                      <div id="articleText">   </div>
                    </body>
                    </html>
                    """;
            Document doc = Jsoup.parse(emptyBodyHtml, "https://www.koreaherald.com");
            NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                    .title("Title")
                    .url("https://www.koreaherald.com/1")
                    .build();

            assertThatThrownBy(() -> koreaHeraldParser.parse(candidate, doc))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_CRAWLING_FAILED));
        }
    }
}
