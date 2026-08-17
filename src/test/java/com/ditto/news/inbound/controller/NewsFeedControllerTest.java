package com.ditto.news.inbound.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.api.NewsFeedController;
import com.ditto.security.SecurityConfig;

@WebMvcTest(
        controllers = NewsFeedController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class NewsFeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsFeedService newsFeedService;

    @Test
    @DisplayName("GET /api/v1/news - 목록 페이징 조회가 성공하면 200 OK와 리스트를 반환한다")
    void getNewsFeedListReturns200() throws Exception {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("K-POP 컴백 대전")
                .slug("k-pop-comeback")
                .summaries(List.of("요약 1", "요약 2"))
                .keywords(List.of("#KPOP"))
                .createdAt(LocalDateTime.now())
                .build();

        given(newsFeedService.getNewsFeeds(anyInt(), anyInt())).willReturn(List.of(feed));

        mockMvc.perform(get("/api/v1/news")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("K-POP 컴백 대전"))
                .andExpect(jsonPath("$.data[0].slug").value("k-pop-comeback"));
    }

    @Test
    @DisplayName("GET /api/v1/news/{id} - ID 단건 조회가 성공하면 200 OK와 상세 본문을 반환한다")
    void getNewsFeedByIdReturns200() throws Exception {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("K-POP 컴백 대전")
                .body("본문 내용")
                .slug("k-pop-comeback")
                .summaries(List.of("요약 1"))
                .build();

        given(newsFeedService.getNewsFeedById(1L)).willReturn(feed);

        mockMvc.perform(get("/api/v1/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newsFeedId").value(1))
                .andExpect(jsonPath("$.data.body").value("본문 내용"));
    }

    @Test
    @DisplayName("GET /api/v1/news/slug/{slug} - Slug 조회가 성공하면 200 OK를 반환한다")
    void getNewsFeedBySlugReturns200() throws Exception {
        NewsFeed feed = NewsFeed.builder()
                .newsFeedId(1L)
                .title("K-POP 컴백 대전")
                .slug("k-pop-slug")
                .build();

        given(newsFeedService.getNewsFeedBySlug("k-pop-slug")).willReturn(feed);

        mockMvc.perform(get("/api/v1/news/slug/k-pop-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("k-pop-slug"));
    }
}
