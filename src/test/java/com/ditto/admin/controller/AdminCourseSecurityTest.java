package com.ditto.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.service.AdminCourseService;
import com.ditto.config.CorsConfig;
import com.ditto.security.LocalHeaderAuthenticationFilter;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 초안은 아직 사람이 안 본 것이고 조사 원문까지 들고 있다. 관리자 밖으로 새면 안 된다 —
 * 경로를 {@code /api/v1/admin/**} 아래에 둔 것이 그 보장이고, 이 테스트가 그것을 지킨다.
 */
@WebMvcTest(AdminCourseController.class)
@Import({SecurityConfig.class, CorsConfig.class, LocalHeaderAuthenticationFilter.class})
@ActiveProfiles("local")
class AdminCourseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCourseService adminCourseService;

    @Test
    @DisplayName("ROLE_ADMIN 사용자는 코스 초안을 조회할 수 있다")
    void adminCanReadDrafts() throws Exception {
        given(adminCourseService.getDrafts()).willReturn(list());

        mockMvc.perform(get("/api/v1/admin/admin-courses")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("ROLE_CUSTOMER 사용자는 코스 초안 조회가 거부된다")
    void customerCannotReadDrafts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/admin-courses")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A004"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 코스 초안 조회가 거부된다")
    void anonymousCannotReadDrafts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/admin-courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 초안 상세도 볼 수 없다")
    void anonymousCannotReadDraftDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/admin-courses/{celebrity}", "카리나"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 서비스 중인 코스 목록도 볼 수 없다")
    void anonymousCannotReadCachedCourses() throws Exception {
        mockMvc.perform(get("/api/v1/admin/admin-courses/cached"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    private AdminCourseListResponse list() throws Exception {
        return AdminCourseListResponse.builder()
                .functionName("ditto-celeb-warm-2")
                .fetchedAt(Instant.parse("2026-08-25T05:20:00Z"))
                .count(1)
                .payload(objectMapper.readTree(
                        "{\"count\":1,\"drafts\":[{\"celebrity\":\"카리나\",\"status\":\"ok\"}]}"))
                .build();
    }
}
