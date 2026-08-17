package com.ditto.news.inbound.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true")
@ActiveProfiles("local")
class NewsFeedAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsFeedService newsFeedService;

    @Test
    @DisplayName("ROLE_ADMIN 권한을 가진 관리자는 PATCH /api/v1/admin/news/{newsId} 수정에 성공한다 (200 OK)")
    void adminCanUpdateNewsFeed() throws Exception {
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
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("ROLE_ADMIN 권한을 가진 관리자는 DELETE /api/v1/admin/news/{newsId} 삭제에 성공한다 (200 OK)")
    void adminCanDeleteNewsFeed() throws Exception {
        doNothing().when(newsFeedService).deleteNewsFeed(1L);

        mockMvc.perform(delete("/api/v1/admin/news/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("일반 사용자(ROLE_CUSTOMER)가 관리자 뉴스피드 수정 시도 시 403 Forbidden 거부된다")
    void customerCannotUpdateNewsFeed() throws Exception {
        NewsFeedUpdateRequest request = NewsFeedUpdateRequest.builder()
                .title("수정된 제목")
                .body("수정된 본문")
                .build();

        mockMvc.perform(patch("/api/v1/admin/news/1")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A004"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 뉴스피드 삭제 시도 시 401 Unauthorized 거부된다")
    void anonymousCannotDeleteNewsFeed() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/news/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("공개 뉴스피드 조회 GET /api/v1/news 는 인증 없이 누구나 접근 가능하다 (200 OK)")
    void publicCanGetNewsFeedsWithoutAuth() throws Exception {
        given(newsFeedService.getNewsFeeds(0, 10)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
