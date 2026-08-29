package com.ditto.recommendation.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.global.common.response.PageResponse;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.recommendation.dto.response.RecommendedCourseResponse;
import com.ditto.recommendation.service.RecommendedCourseService;

@WebMvcTest(RecommendedCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendedCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendedCourseService service;

    @Test
    void forwardsAcceptLanguageToRecommendedCourseService() throws Exception {
        RecommendedCourseResponse item = RecommendedCourseResponse.builder()
                .courseId(7L)
                .name("Brand course")
                .placeNames(List.of())
                .countryCodes(List.of("JP"))
                .build();
        given(service.getRecommended(0, 20, "JP", ContentLanguage.JAPANESE))
                .willReturn(new PageResponse<>(List.of(item), 0, 1L));

        mockMvc.perform(get("/api/v1/courses/recommended")
                        .param("country", "JP")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ja-JP,ja;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Brand course"));

        verify(service).getRecommended(0, 20, "JP", ContentLanguage.JAPANESE);
    }
}
