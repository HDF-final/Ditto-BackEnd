package com.ditto.news.service.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.news.pipeline.model.NewsArticleCandidate;

class NewsKeywordFilterTest {

    private NewsKeywordFilter keywordFilter;

    @BeforeEach
    void setUp() {
        keywordFilter = new NewsKeywordFilter();
    }

    @Test
    @DisplayName("제목 또는 요약에 K-컬처 키워드가 포함된 기사만 1차 통과시키고 matchedKeyword를 설정한다")
    void filtersMatchingCandidates() {
        NewsArticleCandidate candidate1 = NewsArticleCandidate.builder()
                .title("New Jeans Breaks Record on Global Charts with New Single")
                .url("https://koreaherald.com/1")
                .description("The global sensation continues to lead the K-POP wave.")
                .publishedAt(LocalDateTime.now())
                .source("The Korea Herald")
                .build();

        NewsArticleCandidate candidate2 = NewsArticleCandidate.builder()
                .title("2026 K-뷰티 트렌드 분석: 글로벌 올리브영 인기 품목")
                .url("https://koreatimes.co.kr/2")
                .description("한국 화장품 수출 호조세 지속")
                .publishedAt(LocalDateTime.now())
                .source("The Korea Times")
                .build();

        NewsArticleCandidate candidate3 = NewsArticleCandidate.builder()
                .title("국내 반도체 수출 실적 전년 대비 15% 상승")
                .url("https://yna.co.kr/3")
                .description("IT 부품 제조업 경기 회복 조짐")
                .publishedAt(LocalDateTime.now())
                .source("Yonhap News")
                .build();

        List<String> keywords = List.of("K-POP", "K-뷰티", "K-패션", "한국 팝업스토어", "서울 핫플");

        List<NewsArticleCandidate> filtered = keywordFilter.filterByKeywords(
                List.of(candidate1, candidate2, candidate3), keywords);

        assertThat(filtered).hasSize(2);
        assertThat(filtered.get(0).getUrl()).isEqualTo("https://koreaherald.com/1");
        assertThat(filtered.get(0).getMatchedKeyword()).isEqualTo("K-POP");

        assertThat(filtered.get(1).getUrl()).isEqualTo("https://koreatimes.co.kr/2");
        assertThat(filtered.get(1).getMatchedKeyword()).isEqualTo("K-뷰티");
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
    @DisplayName("잘못된 키워드(null, 공백 문자열)는 안전하게 무시되고 유효한 키워드만 필터링에 사용된다")
    void handlesInvalidKeywordsSafely() {
        NewsArticleCandidate candidate1 = NewsArticleCandidate.builder()
                .title("성수동 서울 핫플 팝업스토어 오픈 안내")
                .url("https://example.com/1")
                .description("주말 나들이 추천 장소")
                .build();

        NewsArticleCandidate candidate2 = NewsArticleCandidate.builder()
                .title("일반 날씨 예보 안내")
                .url("https://example.com/2")
                .description("전국 대체로 맑음")
                .build();

        List<String> keywordsWithInvalid = Arrays.asList(null, "", "   ", "서울 핫플");

        List<NewsArticleCandidate> result = keywordFilter.filterByKeywords(
                List.of(candidate1, candidate2), keywordsWithInvalid);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("성수동 서울 핫플 팝업스토어 오픈 안내");
        assertThat(result.get(0).getMatchedKeyword()).isEqualTo("서울 핫플");
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
