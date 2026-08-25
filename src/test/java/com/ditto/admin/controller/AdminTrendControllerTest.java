package com.ditto.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.admin.service.AdminTrendService;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = AdminTrendController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminTrendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTrendService adminTrendService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsTop4Artifact() throws Exception {
        given(adminTrendService.getTop4()).willReturn(artifact("top4"));

        mockMvc.perform(get("/api/v1/admin/trends/top4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.artifact").value("top4"))
                .andExpect(jsonPath("$.data.payload.countries.KR").isArray());
    }

    @Test
    void returnsCandidatesArtifact() throws Exception {
        given(adminTrendService.getCandidates()).willReturn(artifact("candidates"));

        mockMvc.perform(get("/api/v1/admin/trends/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artifact").value("candidates"));
    }

    @Test
    void returnsYoutubeArtifact() throws Exception {
        given(adminTrendService.getYoutube()).willReturn(artifact("youtube"));

        mockMvc.perform(get("/api/v1/admin/trends/youtube"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artifact").value("youtube"));
    }

    private TrendArtifactResponse artifact(String type) throws Exception {
        return TrendArtifactResponse.builder()
                .artifact(type)
                .displayName(type)
                .status("complete")
                .builtAt("2026-08-24T06:48:30Z")
                .warningCount(0)
                .payload(objectMapper.readTree("{\"countries\":{\"KR\":[]}}"))
                .build();
    }
}
