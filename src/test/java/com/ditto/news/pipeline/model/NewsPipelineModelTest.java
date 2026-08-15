package com.ditto.news.pipeline.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsPipelineModelTest {

    @Test
    @DisplayName("NewsArticleCandidate가 Builder 및 Getter를 통해 정상 생성되고 URL 기준 동등성을 만족한다")
    void newsArticleCandidateModelWorks() {
        LocalDateTime now = LocalDateTime.now();
        NewsArticleCandidate candidate1 = NewsArticleCandidate.builder()
                .title("K-POP 글로벌 차트 1위")
                .url("https://koreaherald.com/news/1")
                .source("The Korea Herald")
                .publishedAt(now)
                .description("K-POP 음원 신기록 달성")
                .matchedKeyword("K-POP")
                .build();

        NewsArticleCandidate candidate2 = NewsArticleCandidate.builder()
                .title("다른 제목이지만 동일한 URL")
                .url("https://koreaherald.com/news/1")
                .source("The Korea Herald")
                .publishedAt(now)
                .build();

        assertThat(candidate1.getTitle()).isEqualTo("K-POP 글로벌 차트 1위");
        assertThat(candidate1.getUrl()).isEqualTo("https://koreaherald.com/news/1");
        assertThat(candidate1.getSource()).isEqualTo("The Korea Herald");
        assertThat(candidate1.getPublishedAt()).isEqualTo(now);
        assertThat(candidate1.getDescription()).isEqualTo("K-POP 음원 신기록 달성");
        assertThat(candidate1.getMatchedKeyword()).isEqualTo("K-POP");

        // URL 기준 Equals & HashCode
        assertThat(candidate1).isEqualTo(candidate2);
        assertThat(candidate1.hashCode()).isEqualTo(candidate2.hashCode());
    }

    @Test
    @DisplayName("CrawledNewsArticle이 Builder 및 Getter를 통해 정상 생성된다")
    void crawledNewsArticleModelWorks() {
        LocalDateTime now = LocalDateTime.now();
        CrawledNewsArticle article = CrawledNewsArticle.builder()
                .title("2026 K-뷰티 트렌드")
                .body("한국 뷰티 제품의 인기가 지속되고 있습니다...")
                .url("https://koreatimes.co.kr/beauty/10")
                .source("The Korea Times")
                .publishedAt(now)
                .imageUrl("https://cdn.ditto.test/images/beauty.jpg")
                .build();

        assertThat(article.getTitle()).isEqualTo("2026 K-뷰티 트렌드");
        assertThat(article.getBody()).contains("한국 뷰티 제품");
        assertThat(article.getUrl()).isEqualTo("https://koreatimes.co.kr/beauty/10");
        assertThat(article.getSource()).isEqualTo("The Korea Times");
        assertThat(article.getPublishedAt()).isEqualTo(now);
        assertThat(article.getImageUrl()).isEqualTo("https://cdn.ditto.test/images/beauty.jpg");
    }

    @Test
    @DisplayName("GeneratedNewsFeed가 Builder 및 Getter를 통해 정상 생성된다")
    void generatedNewsFeedModelWorks() {
        GeneratedNewsFeed feed = GeneratedNewsFeed.builder()
                .title("지금 서울에서 가장 핫한 K-패션 팝업 TOP 3")
                .body("이번 주말 가볼 만한 성수동 K-패션 팝업스토어를 소개합니다.")
                .slug("seoul-k-fashion-popup-top3")
                .representativeImageUrl("https://cdn.ditto.test/images/feed-thumb.jpg")
                .keywords(List.of("K-패션", "한국 팝업스토어", "서울 핫플"))
                .build();

        assertThat(feed.getTitle()).isEqualTo("지금 서울에서 가장 핫한 K-패션 팝업 TOP 3");
        assertThat(feed.getBody()).contains("성수동 K-패션");
        assertThat(feed.getSlug()).isEqualTo("seoul-k-fashion-popup-top3");
        assertThat(feed.getRepresentativeImageUrl()).isEqualTo("https://cdn.ditto.test/images/feed-thumb.jpg");
        assertThat(feed.getKeywords()).containsExactly("K-패션", "한국 팝업스토어", "서울 핫플");
    }
}
