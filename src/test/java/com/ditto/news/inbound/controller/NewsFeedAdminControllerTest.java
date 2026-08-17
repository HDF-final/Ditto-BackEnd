package com.ditto.news.inbound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.ditto.news.inbound.rest.api.NewsFeedAdminController;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = NewsFeedAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class NewsFeedAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsFeedService newsFeedService;

    @Test
    @DisplayName("PATCH /api/v1/admin/news/{newsId} - 관리자 뉴스피드 수정 성공 시 200 OK를 반환한다")
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

        mockMvc.perform(patch("/api/v1/admin/news/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/news/{newsId} - 관리자 뉴스피드 삭제 성공 시 200 OK를 반환한다")
    void deleteNewsFeedReturns200() throws Exception {
        doNothing().when(newsFeedService).deleteNewsFeed(1L);

        mockMvc.perform(delete("/api/v1/admin/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
