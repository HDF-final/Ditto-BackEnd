package com.ditto.news.selector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.news.pipeline.model.CrawledNewsArticle;

class DefaultNewsArticleSelectorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-16T12:00:00Z"); // KST 2026-08-16 21:00:00

    private Clock fixedClock;
    private DefaultNewsArticleSelector selector;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
        selector = new DefaultNewsArticleSelector(fixedClock);
        baseTime = LocalDateTime.now(fixedClock);
    }

    @Test
    @DisplayName("URL이 동일한 기사는 점수가 더 높고 최신인 기사 1건만 유지된다")
    void deduplicatesByUrl() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("New Jeans Breaks Global Records with K-POP Single")
                .body("K-pop girl group New Jeans achieves massive success.")
                .url("https://koreaherald.com/news/1")
                .publishedAt(baseTime.minusHours(2))
                .build();

        CrawledNewsArticle article2 = CrawledNewsArticle.builder()
                .title("New Jeans Comeback")
                .body("Brief summary.")
                .url("https://koreaherald.com/news/1") // 동일 URL
                .publishedAt(baseTime.minusHours(10))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(article2, article1), List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("New Jeans Breaks Global Records with K-POP Single");
        assertThat(results.get(0).getUrl()).isEqualTo("https://koreaherald.com/news/1");
    }

    @Test
    @DisplayName("대소문자, 특수문자, 괄호 태그 차이가 있는 정규화 동일 제목의 중복 기사는 1건만 유지된다")
    void deduplicatesByNormalizedTitle() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("[단독] K-POP Star Returns to Stage!")
                .body("Detailed report on the concert.")
                .url("https://koreaherald.com/news/101")
                .publishedAt(baseTime.minusHours(1))
                .build();

        CrawledNewsArticle article2 = CrawledNewsArticle.builder()
                .title("k pop star returns to stage")
                .body("Brief report.")
                .url("https://koreatimes.co.kr/news/202")
                .publishedAt(baseTime.minusHours(5))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(article2, article1), List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUrl()).isEqualTo("https://koreaherald.com/news/101");
        assertThat(results.get(0).getTitle()).isEqualTo("[단독] K-POP Star Returns to Stage!");
    }

    @Test
    @DisplayName("서로 다른 정상 기사는 제목 정규화 후에도 잘못 병합되지 않고 모두 유지된다")
    void preservesDistinctArticles() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("New Jeans Releases Summer K-POP Track")
                .body("Music video details.")
                .url("https://koreaherald.com/news/1")
                .publishedAt(baseTime.minusHours(1))
                .build();

        CrawledNewsArticle article2 = CrawledNewsArticle.builder()
                .title("IVE Releases Summer K-POP Track")
                .body("Concert details.")
                .url("https://koreaherald.com/news/2")
                .publishedAt(baseTime.minusHours(1))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(article1, article2), List.of("K-POP"));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(CrawledNewsArticle::getUrl)
                .containsExactly("https://koreaherald.com/news/1", "https://koreaherald.com/news/2");
    }

    @Test
    @DisplayName("일반 문화, 농업 축제, 생활문화 동아리 등 K-POP과 무관한 기사는 엄격하게 제외된다")
    void filtersOutGenericCultureAndIrrelevantArticles() {
        CrawledNewsArticle kpopArticle = CrawledNewsArticle.builder()
                .title("BTS Announces New World Tour and Album Release")
                .body("Global K-pop phenomenon BTS revealed their stadium tour dates.")
                .url("https://example.com/bts")
                .publishedAt(baseTime.minusHours(3))
                .build();

        CrawledNewsArticle culture1 = CrawledNewsArticle.builder()
                .title("경남 도민예술단, 9월부터 11개 시군 순회공연")
                .body("인구감소지역 도민에게 문화예술 향유 기회를 확대하고자 기획된 공연입니다.")
                .url("https://example.com/cul1")
                .publishedAt(baseTime.minusHours(1))
                .build();

        CrawledNewsArticle culture2 = CrawledNewsArticle.builder()
                .title("이천쌀문화축제, 10월 복하천 수변공원서 열린다")
                .body("가을 쌀 수확을 기념하는 문화 행사입니다.")
                .url("https://example.com/cul2")
                .publishedAt(baseTime.minusHours(1))
                .build();

        CrawledNewsArticle culture3 = CrawledNewsArticle.builder()
                .title("부천 생활문화 동아리 특화 지도 제작")
                .body("시민들이 참여할 수 있는 생활문화 동아리 안내입니다.")
                .url("https://example.com/cul3")
                .publishedAt(baseTime.minusHours(1))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(kpopArticle, culture1, culture2, culture3), List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("BTS Announces New World Tour and Album Release");
    }

    @Test
    @DisplayName("제목에 키워드가 직접 포함된 기사가 본문에만 포함된 기사보다 높은 점수를 받아 우선 정렬된다")
    void titleMatchScoresHigherThanBodyMatch() {
        CrawledNewsArticle bodyOnlyMatch = CrawledNewsArticle.builder()
                .title("Entertainment Industry Growth in 2026")
                .body("The K-POP market has shown strong annual growth.")
                .url("https://example.com/body-only")
                .publishedAt(baseTime.minusHours(1))
                .build();

        CrawledNewsArticle titleMatch = CrawledNewsArticle.builder()
                .title("New Jeans Breaks K-POP Record")
                .body("Detailed report on the milestone.")
                .url("https://example.com/title-match")
                .publishedAt(baseTime.minusHours(2))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(bodyOnlyMatch, titleMatch), List.of("K-POP"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("New Jeans Breaks K-POP Record");
        assertThat(results.get(1).getTitle()).isEqualTo("Entertainment Industry Growth in 2026");
    }

    @Test
    @DisplayName("관련성 점수가 동일할 경우 더 최신에 발행된 기사가 우선 정렬된다")
    void sortsByRecencyWhenScoresAreEqual() {
        CrawledNewsArticle older = CrawledNewsArticle.builder()
                .title("K-POP Special Concert A")
                .body("Concert details A.")
                .url("https://example.com/a")
                .publishedAt(baseTime.minusDays(5))
                .build();

        CrawledNewsArticle newer = CrawledNewsArticle.builder()
                .title("K-POP Special Concert B")
                .body("Concert details B.")
                .url("https://example.com/b")
                .publishedAt(baseTime.minusHours(5))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(older, newer), List.of("K-POP"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("K-POP Special Concert B");
        assertThat(results.get(1).getTitle()).isEqualTo("K-POP Special Concert A");
    }

    @Test
    @DisplayName("14일 이상 오래된 기사는 선별 대상에서 제외된다")
    void filtersOutOutdatedArticles() {
        CrawledNewsArticle recent = CrawledNewsArticle.builder()
                .title("BLACKPINK Releases New K-POP Album")
                .body("Global hit tracks.")
                .url("https://example.com/recent")
                .publishedAt(baseTime.minusDays(3))
                .build();

        CrawledNewsArticle outdated = CrawledNewsArticle.builder()
                .title("BLACKPINK Past Tour Summary")
                .body("K-pop concert review.")
                .url("https://example.com/outdated")
                .publishedAt(baseTime.minusDays(15)) // 15일 전
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(recent, outdated), List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUrl()).isEqualTo("https://example.com/recent");
    }

    @Test
    @DisplayName("publishedAt이 null인 기사는 예외 없이 안전하게 처리되며 동일 점수 시 날짜 있는 기사 뒤에 정렬된다")
    void handlesNullPublishedAtSafely() {
        CrawledNewsArticle dated = CrawledNewsArticle.builder()
                .title("K-POP Global Chart Milestone 1")
                .body("Detailed idol report.")
                .url("https://example.com/dated")
                .publishedAt(baseTime.minusHours(2))
                .build();

        CrawledNewsArticle undated = CrawledNewsArticle.builder()
                .title("K-POP Global Chart Milestone 2")
                .body("Detailed idol report.")
                .url("https://example.com/undated")
                .publishedAt(null)
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(undated, dated), List.of("K-POP"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("K-POP Global Chart Milestone 1");
        assertThat(results.get(1).getTitle()).isEqualTo("K-POP Global Chart Milestone 2");
    }

    @Test
    @DisplayName("후보 기사가 최대 선별 개수(5개)를 초과할 경우 상위 5개만 반환한다")
    void limitsToMaxArticles() {
        List<CrawledNewsArticle> articles = List.of(
                createArticle(1, "K-POP 1", 1),
                createArticle(2, "K-POP 2", 2),
                createArticle(3, "K-POP 3", 3),
                createArticle(4, "K-POP 4", 4),
                createArticle(5, "K-POP 5", 5),
                createArticle(6, "K-POP 6", 6),
                createArticle(7, "K-POP 7", 7)
        );

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(articles, List.of("K-POP"));

        assertThat(results).hasSize(5);
        assertThat(results.get(0).getTitle()).isEqualTo("K-POP 1");
        assertThat(results.get(4).getTitle()).isEqualTo("K-POP 5");
    }

    @Test
    @DisplayName("후보 기사가 최대 선별 개수(5개)보다 적으면 존재하는 기사만 모두 반환한다")
    void returnsAllWhenLessThanMax() {
        List<CrawledNewsArticle> articles = List.of(
                createArticle(1, "K-POP Single 1", 1),
                createArticle(2, "K-POP Single 2", 2),
                createArticle(3, "K-POP Single 3", 3)
        );

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(articles, List.of("K-POP"));

        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("null 목록, 빈 목록, null 토픽, 빈 토픽 입력 시 안전하게 빈 목록을 반환한다")
    void handlesNullAndEmptyInputsGracefully() {
        CrawledNewsArticle article = createArticle(1, "K-POP 1", 1);

        assertThat(selector.selectRelevantArticles(null, List.of("K-POP"))).isEmpty();
        assertThat(selector.selectRelevantArticles(Collections.emptyList(), List.of("K-POP"))).isEmpty();
        assertThat(selector.selectRelevantArticles(List.of(article), null)).isEmpty();
        assertThat(selector.selectRelevantArticles(List.of(article), Collections.emptyList())).isEmpty();
        assertThat(selector.selectRelevantArticles(List.of(article), Arrays.asList(null, "", "   "))).isEmpty();
    }

    @Test
    @DisplayName("기사 목록에 null 항목이나 본문/제목/URL이 빈 항목이 포함되어 있어도 안전하게 건너뛴다")
    void handlesNullOrMalformedArticlesInList() {
        CrawledNewsArticle valid = createArticle(1, "K-POP Global Record", 1);
        CrawledNewsArticle nullUrl = CrawledNewsArticle.builder().title("Title").body("Body").url(null).build();
        CrawledNewsArticle blankBody = CrawledNewsArticle.builder().title("Title").body("   ").url("https://ex.com/2").build();
        CrawledNewsArticle blankTitle = CrawledNewsArticle.builder().title("   ").body("Body").url("https://ex.com/3").build();

        List<CrawledNewsArticle> mixed = Arrays.asList(null, valid, nullUrl, blankBody, blankTitle);

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(mixed, List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("K-POP Global Record");
    }

    @Test
    @DisplayName("보조 표현(아이돌, kpop, 걸그룹, aespa 등)이 포함된 기사도 정상 매칭되어 선별된다")
    void matchesRelatedAuxiliaryTerms() {
        CrawledNewsArticle articleWithIdol = CrawledNewsArticle.builder()
                .title("차세대 글로벌 아이돌 그룹 데뷔 쇼케이스")
                .body("신인 걸그룹 aespa가 화려하게 데뷔했습니다.")
                .url("https://example.com/idol")
                .publishedAt(baseTime.minusHours(1))
                .build();

        List<CrawledNewsArticle> results = selector.selectRelevantArticles(
                List.of(articleWithIdol), List.of("K-POP"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("차세대 글로벌 아이돌 그룹 데뷔 쇼케이스");
    }

    private CrawledNewsArticle createArticle(int id, String title, int hoursAgo) {
        return CrawledNewsArticle.builder()
                .title(title)
                .body("본문 내용 " + title + " 상세 정보입니다.")
                .url("https://example.com/news/" + id)
                .publishedAt(baseTime.minusHours(hoursAgo))
                .source("테스트 언론사")
                .build();
    }
}
