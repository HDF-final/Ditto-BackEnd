package com.ditto.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.admin.service.AdminTrendService;
import com.ditto.config.CorsConfig;
import com.ditto.security.LocalHeaderAuthenticationFilter;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AdminTrendController.class)
@Import({SecurityConfig.class, CorsConfig.class, LocalHeaderAuthenticationFilter.class})
@ActiveProfiles("local")
class AdminTrendSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTrendService adminTrendService;

    @Test
    @DisplayName("ROLE_ADMIN 사용자는 트렌드 산출물을 조회할 수 있다")
    void adminCanReadTrendArtifact() throws Exception {
        given(adminTrendService.getTop10()).willReturn(artifact());

        mockMvc.perform(get("/api/v1/admin/trends/top10")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.artifact").value("top10"));
    }

    @Test
    @DisplayName("ROLE_CUSTOMER 사용자는 트렌드 산출물 조회가 거부된다")
    void customerCannotReadTrendArtifact() throws Exception {
        mockMvc.perform(get("/api/v1/admin/trends/top10")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A004"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 트렌드 산출물 조회가 거부된다")
    void anonymousCannotReadTrendArtifact() throws Exception {
        mockMvc.perform(get("/api/v1/admin/trends/top10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A001"));
    }

    private TrendArtifactResponse artifact() throws Exception {
        return TrendArtifactResponse.builder()
                .artifact("top10")
                .displayName("국가별 TOP 10")
                .status("complete")
                .builtAt("2026-08-24T06:48:30Z")
                .warningCount(0)
                .payload(objectMapper.readTree("{\"countries\":{\"KR\":[]}}"))
                .build();
    }
}
