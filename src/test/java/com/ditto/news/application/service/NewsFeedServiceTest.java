package com.ditto.news.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.domain.NewsFeed;

@ExtendWith(MockitoExtension.class)
class NewsFeedServiceTest {

    @Mock
    private NewsFeedRepository newsFeedRepository;

    private NewsFeedService newsFeedService;

    @BeforeEach
    void setUp() {
        newsFeedService = new NewsFeedService(newsFeedRepository);
    }

    @Test
    @DisplayName("뉴스피드 목록을 정상적으로 조회하여 도메인 엔티티 목록으로 반환한다")
    void getNewsFeedListSuccessfully() {
        NewsFeed feed1 = NewsFeed.builder()
                .newsFeedId(1L)
                .title("Title 1")
                .slug("slug-1")
                .summaries(List.of("요약 1"))
                .keywords(List.of("#KPOP"))
                .createdAt(LocalDateTime.now())
                .build();

        given(newsFeedRepository.findAll(0, 10)).willReturn(List.of(feed1));

        List<NewsFeed> result = newsFeedService.getNewsFeeds(0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(result.get(0).getSlug()).isEqualTo("slug-1");
    }

    @Test
    @DisplayName("PK ID로 뉴스피드 상세 도메인 엔티티를 정상 조회한다")
    void getNewsFeedByIdSuccessfully() {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("Title 1")
                .body("Body 1")
                .slug("slug-1")
                .summaries(List.of("요약 1", "요약 2"))
                .keywords(List.of("#KPOP"))
                .createdAt(LocalDateTime.now())
                .build();

        given(newsFeedRepository.findById(1L)).willReturn(Optional.of(feed));

        NewsFeed response = newsFeedService.getNewsFeedById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Title 1");
        assertThat(response.getBody()).isEqualTo("Body 1");
        assertThat(response.getSummaries()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 시 NEWS_FEED_NOT_FOUND 예외를 던진다")
    void getNewsFeedByIdThrowsWhenNotFound() {
        given(newsFeedRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> newsFeedService.getNewsFeedById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("URL Slug로 뉴스피드 상세 도메인 엔티티를 정상 조회한다")
    void getNewsFeedBySlugSuccessfully() {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("Title 1")
                .slug("k-pop-slug")
                .build();

        given(newsFeedRepository.findBySlug("k-pop-slug")).willReturn(Optional.of(feed));

        NewsFeed response = newsFeedService.getNewsFeedBySlug("k-pop-slug");

        assertThat(response).isNotNull();
        assertThat(response.getSlug()).isEqualTo("k-pop-slug");
    }

    @Test
    @DisplayName("뉴스피드 내용을 수정한다")
    void updateNewsFeedSuccessfully() {
        NewsFeed existing = NewsFeed.builder()
                .newsFeedId(1L)
                .title("Old Title")
                .body("Old Body")
                .slug("slug-1")
                .build();

        given(newsFeedRepository.findById(1L)).willReturn(Optional.of(existing));

        NewsFeed updated = newsFeedService.updateNewsFeed(
                1L,
                "New Title",
                "New Body",
                "https://img.yna.co.kr/photo.jpg",
                List.of("새 요약"),
                List.of("#NEW")
        );

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getBody()).isEqualTo("New Body");
        verify(newsFeedRepository).update(any(NewsFeed.class));
    }

    @Test
    @DisplayName("뉴스피드를 삭제한다")
    void deleteNewsFeedSuccessfully() {
        NewsFeed existing = NewsFeed.builder().newsFeedId(1L).build();
        given(newsFeedRepository.findById(1L)).willReturn(Optional.of(existing));

        newsFeedService.deleteNewsFeed(1L);

        verify(newsFeedRepository).deleteById(1L);
    }
}
