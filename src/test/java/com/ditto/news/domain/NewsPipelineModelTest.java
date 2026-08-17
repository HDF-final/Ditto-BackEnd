package com.ditto.news.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsPipelineModelTest {

    @Test
    @DisplayName("NewsArticleCandidate 모델 객체가 정상 생성된다")
    void testNewsArticleCandidate() {
        LocalDateTime now = LocalDateTime.now();
        NewsArticleCandidate candidate = NewsArticleCandidate.builder()
                .title("BTS New Album")
                .url("https://example.com/news/1")
                .source("Yonhap News")
                .publishedAt(now)
                .description("BTS released new album")
                .matchedKeyword("K-POP")
                .build();

        assertThat(candidate.getTitle()).isEqualTo("BTS New Album");
        assertThat(candidate.getUrl()).isEqualTo("https://example.com/news/1");
        assertThat(candidate.getSource()).isEqualTo("Yonhap News");
        assertThat(candidate.getPublishedAt()).isEqualTo(now);
        assertThat(candidate.getDescription()).isEqualTo("BTS released new album");
        assertThat(candidate.getMatchedKeyword()).isEqualTo("K-POP");
    }

    @Test
    @DisplayName("CrawledNewsArticle 모델 객체가 정상 생성된다")
    void testCrawledNewsArticle() {
        LocalDateTime now = LocalDateTime.now();
        CrawledNewsArticle article = CrawledNewsArticle.builder()
                .title("K-POP Global Popularity")
                .body("Full article body text here...")
                .url("https://example.com/news/2")
                .source("Korea Herald")
                .publishedAt(now)
                .imageUrl("https://example.com/img.jpg")
                .build();

        assertThat(article.getTitle()).isEqualTo("K-POP Global Popularity");
        assertThat(article.getBody()).isEqualTo("Full article body text here...");
        assertThat(article.getUrl()).isEqualTo("https://example.com/news/2");
        assertThat(article.getSource()).isEqualTo("Korea Herald");
        assertThat(article.getPublishedAt()).isEqualTo(now);
        assertThat(article.getImageUrl()).isEqualTo("https://example.com/img.jpg");
    }

    @Test
    @DisplayName("GeneratedNewsFeed 모델 객체가 정상 생성된다")
    void testGeneratedNewsFeed() {
        GeneratedNewsFeed feed = GeneratedNewsFeed.builder()
                .title("Today's K-POP Feed")
                .body("Summarized feed body...")
                .slug("k-pop-today")
                .representativeImageUrl("https://example.com/rep.jpg")
                .keywords(List.of("K-POP", "BTS"))
                .build();

        assertThat(feed.getTitle()).isEqualTo("Today's K-POP Feed");
        assertThat(feed.getBody()).isEqualTo("Summarized feed body...");
        assertThat(feed.getSlug()).isEqualTo("k-pop-today");
        assertThat(feed.getRepresentativeImageUrl()).isEqualTo("https://example.com/rep.jpg");
        assertThat(feed.getKeywords()).containsExactly("K-POP", "BTS");
    }
}
