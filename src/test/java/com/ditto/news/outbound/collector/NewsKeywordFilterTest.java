package com.ditto.news.outbound.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.news.domain.NewsArticleCandidate;

class NewsKeywordFilterTest {

    private NewsKeywordFilter keywordFilter;

    @BeforeEach
    void setUp() {
        keywordFilter = new NewsKeywordFilter();
    }

    @Test
    @DisplayName("K-POP 키워드 및 정밀 연관어(BTS, 아이돌, 뉴진스 등)가 포함된 기사만 1차 후보로 통과시킨다")
    void filtersKpopCandidates() {
        NewsArticleCandidate candidate1 = NewsArticleCandidate.builder()
                .title("New Jeans Breaks Record on Global Charts with New Single")
                .url("https://koreaherald.com/1")
                .description("The global sensation continues to lead the music wave.")
                .publishedAt(LocalDateTime.now())
                .source("The Korea Herald")
                .build();

        NewsArticleCandidate candidate2 = NewsArticleCandidate.builder()
                .title("방탄소년단(BTS) 월드투어 일정 공개")
                .url("https://koreatimes.co.kr/2")
                .description("내년 스타디움 투어 돌입")
                .publishedAt(LocalDateTime.now())
                .source("The Korea Times")
                .build();

        NewsArticleCandidate candidate3 = NewsArticleCandidate.builder()
                .title("신인 걸그룹 쇼케이스 개최")
                .url("https://yna.co.kr/3")
                .description("데뷔 앨범 발표")
                .publishedAt(LocalDateTime.now())
                .source("Yonhap News")
                .build();

        List<NewsArticleCandidate> filtered = keywordFilter.filterByKeywords(
                List.of(candidate1, candidate2, candidate3), List.of("K-POP"));

        assertThat(filtered).hasSize(3);
        assertThat(filtered).extracting(NewsArticleCandidate::getMatchedKeyword)
                .containsOnly("K-POP");
    }

    @Test
    @DisplayName("문화, 공연, 예술, 축제 등 일반적인 단어만 존재하는 기사는 K-POP 후보에서 엄격하게 제외된다")
    void excludesGenericCultureAndFestivalArticles() {
        NewsArticleCandidate culture1 = NewsArticleCandidate.builder()
                .title("경남 도민예술단, 9월부터 11개 시군 순회공연…'공연예술 향유'")
                .url("https://yna.co.kr/cul1")
                .description("지역 문화예술 행사 안내")
                .build();

        NewsArticleCandidate culture2 = NewsArticleCandidate.builder()
                .title("이천쌀문화축제, 10월 14~18일 복하천 수변공원서 열린다")
                .url("https://yna.co.kr/cul2")
                .description("가을 농업 축제 행사")
                .build();

        NewsArticleCandidate culture3 = NewsArticleCandidate.builder()
                .title("독립기념관 광복절 경축문화행사 개최")
                .url("https://yna.co.kr/cul3")
                .description("평화의 문화 기념식")
                .build();

        NewsArticleCandidate culture4 = NewsArticleCandidate.builder()
                .title("부천 생활문화 동아리 특화 지도 제작")
                .url("https://yna.co.kr/cul4")
                .description("문화재단 동아리 안내")
                .build();

        List<NewsArticleCandidate> filtered = keywordFilter.filterByKeywords(
                List.of(culture1, culture2, culture3, culture4), List.of("K-POP"));

        assertThat(filtered).isEmpty();
    }

    @Test
    @DisplayName("후보 목록이 null이거나 비어있으면 빈 목록을 반환한다")
    void returnsEmptyWhenCandidatesNullOrEmpty() {
        List<String> keywords = List.of("K-POP");

        assertThat(keywordFilter.filterByKeywords(null, keywords)).isEmpty();
        assertThat(keywordFilter.filterByKeywords(Collections.emptyList(), keywords)).isEmpty();
    }

    @Test
    @DisplayName("키워드 목록이 null이거나 비어있으면 빈 목록을 반환한다")
    void returnsEmptyWhenKeywordsNullOrEmpty() {
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("K-POP news")
                .url("https://example.com/1")
                .build();

        assertThat(keywordFilter.filterByKeywords(List.of(candidate), null)).isEmpty();
        assertThat(keywordFilter.filterByKeywords(List.of(candidate), Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("잘못된 키워드(null, 공백 문자열)는 안전하게 무시된다")
    void handlesInvalidKeywordsSafely() {
        NewsArticleCandidate candidate1 = NewsArticleCandidate.builder()
                .title("블랙핑크 신곡 글로벌 차트 진입")
                .url("https://example.com/1")
                .description("K-POP 걸그룹 신기록")
                .build();

        NewsArticleCandidate candidate2 = NewsArticleCandidate.builder()
                .title("일반 날씨 예보 안내")
                .url("https://example.com/2")
                .description("전국 대체로 맑음")
                .build();

        List<String> keywordsWithInvalid = Arrays.asList(null, "", "   ", "K-POP");

        List<NewsArticleCandidate> result = keywordFilter.filterByKeywords(
                List.of(candidate1, candidate2), keywordsWithInvalid);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("블랙핑크 신곡 글로벌 차트 진입");
        assertThat(result.get(0).getMatchedKeyword()).isEqualTo("K-POP");
    }

    @Test
    @DisplayName("키워드 대소문자를 구분하지 않고 매칭된다")
    void matchesCaseInsensitively() {
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("The Rise of k-pop in North America")
                .url("https://example.com/kpop")
                .description("An in-depth report on korean music trends")
                .build();

        List<NewsArticleCandidate> result = keywordFilter.filterByKeywords(
                List.of(candidate), List.of("K-POP"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatchedKeyword()).isEqualTo("K-POP");
    }
}
