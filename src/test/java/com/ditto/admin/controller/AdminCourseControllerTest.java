package com.ditto.admin.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCourseRunResponse;
import com.ditto.admin.service.AdminCourseService;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = AdminCourseController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminCourseService adminCourseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("초안 목록은 머리말만 돌려준다")
    void returnsDraftList() throws Exception {
        given(adminCourseService.getDrafts()).willReturn(list());

        mockMvc.perform(get("/api/v1/admin/admin-courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.functionName").value("ditto-celeb-warm-2"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.payload.drafts[0].celebrity").value("장원영"))
                .andExpect(jsonPath("$.data.payload.drafts[1].celebrity").value("카리나"));
    }

    @Test
    @DisplayName("초안 상세는 코스 전문을 payload 에 그대로 싣는다")
    void returnsDraftDetail() throws Exception {
        given(adminCourseService.getDraft("카리나")).willReturn(detail());

        mockMvc.perform(get("/api/v1/admin/admin-courses/{celebrity}", "카리나"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.celebrity").value("카리나"))
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.data.shape").value("매장 3 · 카페 1 · 여가 1"))
                .andExpect(jsonPath("$.data.placeCount").value(1))
                .andExpect(jsonPath("$.data.warningCount").value(2))
                .andExpect(jsonPath("$.data.payload.places[0].place_name").value("프라다"))
                .andExpect(jsonPath("$.data.payload.places[0].image.kind").value("evidence"));
    }

    @Test
    @DisplayName("오늘 실행 상황을 돌려준다")
    void returnsRunStatus() throws Exception {
        given(adminCourseService.getRunStatus()).willReturn(run());

        mockMvc.perform(get("/api/v1/admin/admin-courses/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-08-25"))
                .andExpect(jsonPath("$.data.queued").value(0))
                .andExpect(jsonPath("$.data.doneCount").value(2));
    }

    @Test
    @DisplayName("/run 은 인물 이름으로 새지 않는다 — 고정 경로가 변수보다 구체적이다")
    void runPathBeatsCelebrityPath() throws Exception {
        given(adminCourseService.getRunStatus()).willReturn(run());

        mockMvc.perform(get("/api/v1/admin/admin-courses/run"))
                .andExpect(status().isOk());

        then(adminCourseService).should().getRunStatus();
        then(adminCourseService).should(org.mockito.Mockito.never()).getDraft(anyString());
    }

    @Test
    @DisplayName("초안이 없으면 404 로 알려 준다")
    void missingDraftIsNotFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.CELEB_DRAFT_NOT_FOUND))
                .given(adminCourseService).getDraft("없는사람");

        mockMvc.perform(get("/api/v1/admin/admin-courses/{celebrity}", "없는사람"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CD001"));
    }

    @Test
    @DisplayName("람다를 못 읽으면 502 로 알려 준다 — 빈 초안을 성공으로 내보내지 않는다")
    void unreadableLambdaIsBadGateway() throws Exception {
        willThrow(new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED))
                .given(adminCourseService).getDrafts();

        mockMvc.perform(get("/api/v1/admin/admin-courses"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CD002"));
    }

    private AdminCourseListResponse list() throws Exception {
        return AdminCourseListResponse.builder()
                .functionName("ditto-celeb-warm-2")
                .fetchedAt(Instant.parse("2026-08-25T05:20:00Z"))
                .count(2)
                .payload(objectMapper.readTree("""
                        {"count":2,"drafts":[
                          {"celebrity":"장원영","kind":"PERSON","status":"ok",
                           "shape":"매장 3 · 카페 1 · 여가 1","places":5,"warnings":1,
                           "built_at":"2026-08-25T14:16:44","ttl":85879},
                          {"celebrity":"카리나","kind":"PERSON","status":"ok",
                           "shape":"매장 3 · 카페 1 · 여가 1","places":5,"warnings":2,
                           "built_at":"2026-08-25T14:15:10","ttl":85785}]}
                        """))
                .build();
    }

    private AdminCourseDetailResponse detail() throws Exception {
        return AdminCourseDetailResponse.builder()
                .functionName("ditto-celeb-warm-2")
                .fetchedAt(Instant.parse("2026-08-25T05:20:00Z"))
                .celebrity("카리나")
                .kind("PERSON")
                .status("ok")
                .shape("매장 3 · 카페 1 · 여가 1")
                .builtAt("2026-08-25T14:15:10")
                .placeCount(1)
                .warningCount(2)
                .payload(objectMapper.readTree("""
                        {"celebrity":"카리나","status":"ok","reply":"카리나가 선호하는 브랜드…",
                         "warnings":["메종 마르지엘라 — 사진 없음","코스 모양이 어긋났다"],
                         "places":[{"slot_id":103,"kind":"매장","navigation_key":"1F_STORE_0035",
                           "place_name":"프라다","reason_kind":"evidence",
                           "evidence":{"brand":"프라다","article":"https://issuepicker.com/news/26308"},
                           "image":{"kind":"evidence","url":"https://issuepicker.com/x.jpg",
                                    "caption":"카리나 × 프라다"}}]}
                        """))
                .build();
    }

    private AdminCourseRunResponse run() throws Exception {
        return AdminCourseRunResponse.builder()
                .functionName("ditto-celeb-warm-2")
                .fetchedAt(Instant.parse("2026-08-25T05:20:00Z"))
                .date("2026-08-25")
                .queued(0)
                .doneCount(2)
                .payload(objectMapper.readTree("""
                        {"date":"2026-08-25","queued":0,
                         "done":{"카리나":"ok · 매장 3 · 카페 1 · 여가 1",
                                 "장원영":"ok · 매장 3 · 카페 1 · 여가 1"}}
                        """))
                .build();
    }
}
