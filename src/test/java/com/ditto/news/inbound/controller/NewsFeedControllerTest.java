package com.ditto.news.inbound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.api.NewsFeedController;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("PUT /api/v1/news/{id} - 뉴스피드 수정 성공 시 200 OK를 반환한다")
    void updateNewsFeedReturns200() throws Exception {
        NewsFeedUpdateRequest request = NewsFeedUpdateRequest.builder()
                .title("수정된 제목")
                .body("수정된 본문")
                .summaries(List.of("새 요약"))
                .keywords(List.of("#NEW"))
                .build();

        NewsFeed updated = NewsFeed.builder()
                .newsFeedId(1L)
                .title("수정된 제목")
                .body("수정된 본문")
                .build();

        given(newsFeedService.updateNewsFeed(eq(1L), eq("수정된 제목"), eq("수정된 본문"), any(), any(), any()))
                .willReturn(updated);

        mockMvc.perform(put("/api/v1/news/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("DELETE /api/v1/news/{id} - 뉴스피드 삭제 성공 시 200 OK를 반환한다")
    void deleteNewsFeedReturns200() throws Exception {
        doNothing().when(newsFeedService).deleteNewsFeed(1L);

        mockMvc.perform(delete("/api/v1/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
