package com.ditto.news.outbound.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.outbound.repository.entity.NewsFeedRow;
import com.ditto.news.outbound.repository.mapper.NewsFeedMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NewsFeedRepositoryImplTest {

    @Mock
    private NewsFeedMapper newsFeedMapper;

    private ObjectMapper objectMapper;
    private NewsFeedRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new NewsFeedRepositoryImpl(newsFeedMapper, objectMapper);
    }

    @Test
    @DisplayName("GeneratedNewsFeed를 전달받아 NewsFeedMapper.insert를 호출하고 NewsFeed 도메인 객체를 반환한다")
    void savesGeneratedNewsFeedSuccessfully() {
        GeneratedNewsFeed generated = GeneratedNewsFeed.builder()
                .title("K-POP 서머 차트 돌풍")
                .slug("k-pop-1234")
                .representativeImageUrl("https://img.yna.co.kr/photo.jpg")
                .body("본문 내용...")
                .summaries(List.of("요약 1", "요약 2", "요약 3"))
                .keywords(List.of("#KPOP", "#BTS"))
                .build();

        NewsFeed saved = repository.save(generated);

        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("K-POP 서머 차트 돌풍");
        assertThat(saved.getSlug()).isEqualTo("k-pop-1234");
        assertThat(saved.getRepresentativeImageUrl()).isEqualTo("https://img.yna.co.kr/photo.jpg");
        assertThat(saved.getSummaries()).containsExactly("요약 1", "요약 2", "요약 3");
        assertThat(saved.getKeywords()).containsExactly("#KPOP", "#BTS");

        verify(newsFeedMapper).insert(
                eq("K-POP 서머 차트 돌풍"),
                eq("k-pop-1234"),
                eq("https://img.yna.co.kr/photo.jpg"),
                eq("본문 내용..."),
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    @DisplayName("findById 조회 시 NewsFeedRow를 NewsFeed 도메인 엔티티로 매핑하여 반환한다")
    void findByIdSuccessfully() {
        NewsFeedRow row = NewsFeedRow.builder()
                .newsFeedId(10L)
                .title("Title")
                .slug("slug-10")
                .body("Body")
                .summary("[\"요약1\",\"요약2\"]")
                .keywords("[\"#KPOP\"]")
                .sourceUrl("https://example.com/news")
                .createdAt(LocalDateTime.now())
                .build();

        given(newsFeedMapper.findById(10L)).willReturn(Optional.of(row));

        Optional<NewsFeed> result = repository.findById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getNewsFeedId()).isEqualTo(10L);
        assertThat(result.get().getSummaries()).containsExactly("요약1", "요약2");
        assertThat(result.get().getKeywords()).containsExactly("#KPOP");
        assertThat(result.get().getSourceUrl()).isEqualTo("https://example.com/news");
    }

    @Test
    @DisplayName("findBySlug 조회 시 정상적으로 도메인 엔티티를 반환한다")
    void findBySlugSuccessfully() {
        NewsFeedRow row = NewsFeedRow.builder()
                .newsFeedId(10L)
                .title("Title")
                .slug("test-slug")
                .build();

        given(newsFeedMapper.findBySlug("test-slug")).willReturn(Optional.of(row));

        Optional<NewsFeed> result = repository.findBySlug("test-slug");

        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo("test-slug");
    }

    @Test
    @DisplayName("findAll 조회 시 목록을 도메인 엔티티 리스트로 변환한다")
    void findAllSuccessfully() {
        NewsFeedRow row = NewsFeedRow.builder()
                .newsFeedId(1L)
                .title("T")
                .build();

        given(newsFeedMapper.findAll(0, 10)).willReturn(List.of(row));

        List<NewsFeed> list = repository.findAll(0, 10);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("T");
    }

    @Test
    @DisplayName("update와 deleteById가 정상적으로 Mapper를 호출한다")
    void updateAndDeleteSuccessfully() {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("Updated Title")
                .body("Updated Body")
                .summaries(List.of("요약"))
                .keywords(List.of("#TAG"))
                .sourceUrl("https://example.com/news")
                .build();

        repository.update(feed);
        verify(newsFeedMapper).update(eq(1L), eq("Updated Title"), eq("Updated Body"), any(), anyString(), anyString(), any());

        repository.deleteById(1L);
        verify(newsFeedMapper).deleteById(1L);
    }
}
