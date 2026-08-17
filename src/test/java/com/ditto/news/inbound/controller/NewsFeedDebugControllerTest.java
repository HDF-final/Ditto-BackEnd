package com.ditto.news.inbound.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.ditto.news.inbound.rest.api.NewsFeedDebugController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = NewsFeedDebugController.class)
@AutoConfigureMockMvc(addFilters = false)
class NewsFeedDebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsFeedPipelineService pipelineService;

    @Test
    @DisplayName("POST /api/v1/news/debug/pipeline 호출 시 파이프라인 디버그 응답을 정상 반환한다")
    void runsPipelineDebugSuccessfully() throws Exception {
        String topic = "K-POP";
        CrawledNewsArticle article = CrawledNewsArticle.builder()
                .title("New Jeans World Tour")
                .url("https://www.yna.co.kr/view/1")
                .source("Yonhap News")
                .build();

        GeneratedNewsFeed feed = GeneratedNewsFeed.builder()
                .title("[K-POP] New Jeans World Tour")
                .body("Summary content")
                .slug("k-pop-slug")
                .build();

        NewsPipelineDebugResponse debugResponse = NewsPipelineDebugResponse.builder()
                .topic(topic)
                .candidateCount(10)
                .crawledCount(8)
                .selectedCount(1)
                .selectedArticles(List.of(article))
                .generatedFeed(feed)
                .build();

        given(pipelineService.executePipelineWithDebug(eq(topic))).willReturn(debugResponse);

        mockMvc.perform(post("/api/v1/news/debug/pipeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"K-POP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topic").value("K-POP"))
                .andExpect(jsonPath("$.data.candidateCount").value(10))
                .andExpect(jsonPath("$.data.crawledCount").value(8))
                .andExpect(jsonPath("$.data.selectedCount").value(1))
                .andExpect(jsonPath("$.data.selectedArticles[0].title").value("New Jeans World Tour"))
                .andExpect(jsonPath("$.data.generatedFeed.title").value("[K-POP] New Jeans World Tour"));
    }
}
