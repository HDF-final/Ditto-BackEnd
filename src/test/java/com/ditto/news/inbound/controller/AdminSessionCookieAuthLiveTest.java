package com.ditto.news.inbound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.auth.dto.request.LoginRequest;
import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true")
class AdminSessionCookieAuthLiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsFeedRepository newsFeedRepository;

    @Test
    @Transactional
    @DisplayName("[세션 인증 실시간 검증] admin@naver.com 로그인 -> 세션 획득 -> 관리자 뉴스 수정 성공")
    void testAdminLoginAndSessionAuth() throws Exception {
        // 0. 테스트용 임시 뉴스피드 생성
        NewsFeed tempFeed = NewsFeed.builder()
                .title("임시 테스트 뉴스피드")
                .slug("temp-auth-test-" + System.currentTimeMillis())
                .representativeImageUrl("https://img.test.com/temp.jpg")
                .body("임시 테스트 본문")
                .summaries(List.of("임시 요약 1"))
                .keywords(List.of("#TEMP"))
                .build();
        NewsFeed savedFeed = newsFeedRepository.save(tempFeed);
        Long targetFeedId = savedFeed.getNewsFeedId();

        // 1. admin@naver.com 로그인 요청
        LoginRequest loginRequest = LoginRequest.builder()
                .userEmail("admin@naver.com")
                .password("12341234")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@naver.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andReturn();

        // 2. 발급된 인증 세션(HttpSession) 추출
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertThat(session).isNotNull();

        // 3. 획득한 세션만 실어서 관리자 뉴스피드 수정 요청 (PATCH /api/v1/admin/news/{newsId})
        NewsFeedUpdateRequest updateRequest = NewsFeedUpdateRequest.builder()
                .title("[관리자 세션 쿠키 수정 완료] 8월 컴백 대전")
                .body("세션 쿠키 인증을 통해 성공적으로 수정된 본문입니다.")
                .summaries(List.of("1. 세션 쿠키 인증 성공", "2. ROLE_ADMIN 확인 완료", "3. 뉴스피드 수정 성공"))
                .keywords(List.of("#ADMIN", "#COOKIE_AUTH", "#DITTO"))
                .build();

        mockMvc.perform(patch("/api/v1/admin/news/" + targetFeedId)
                        .session(session) // ⭐️ 세션 전달
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("[관리자 세션 쿠키 수정 완료] 8월 컴백 대전"));

        // 4. 세션 없이 요청 시 401 Unauthorized 차단 검증
        mockMvc.perform(patch("/api/v1/admin/news/" + targetFeedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
