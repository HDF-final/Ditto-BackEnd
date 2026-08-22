package com.ditto.aicourse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.aicourse.dto.request.CourseChatRequest;
import com.ditto.aicourse.dto.response.CourseChatResponse;
import com.ditto.aicourse.service.AiCourseRecommendationService;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.security.AuthUser;

@WebMvcTest(AiCourseRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiCourseRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiCourseRecommendationService aiCourseRecommendationService;

    @Test
    @DisplayName("Accept-Language를 AI 추천 서비스 언어로 전달한다")
    void forwardsRequestedLanguage() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        given(aiCourseRecommendationService.chat(
                eq(2L), any(CourseChatRequest.class), eq(ContentLanguage.ENGLISH)))
                .willReturn(CourseChatResponse.builder()
                        .sessionId("session-1")
                        .reply("Your course is ready.")
                        .turn(1)
                        .places(List.of())
                        .build());

        mockMvc.perform(post("/api/v1/ai/course-recommendations/chat")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Make me a K-pop course\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("Your course is ready."));

        verify(aiCourseRecommendationService).chat(
                eq(2L), any(CourseChatRequest.class), eq(ContentLanguage.ENGLISH));
    }
}
