package com.ditto.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.admin.client.CelebDraftClient;
import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCourseRunResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AdminCourseServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CelebDraftClient celebDraftClient;
    private AdminCourseService service;

    @BeforeEach
    void setUp() {
        celebDraftClient = mock(CelebDraftClient.class);
        given(celebDraftClient.getFunctionName()).willReturn("ditto-celeb-warm-2");
        service = new AdminCourseService(celebDraftClient);
    }

    @Test
    @DisplayName("목록은 초안 수를 세어 머리말에 올린다")
    void countsDrafts() {
        given(celebDraftClient.listDrafts()).willReturn(json("""
                {"count":2,"drafts":[{"celebrity":"카리나"},{"celebrity":"장원영"}]}
                """));

        AdminCourseListResponse response = service.getDrafts();

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getFunctionName()).isEqualTo("ditto-celeb-warm-2");
        assertThat(response.getFetchedAt()).isNotNull();
        assertThat(response.getPayload().path("drafts")).hasSize(2);
    }

    @Test
    @DisplayName("상세는 장소 수와 경고 수를 세어 머리말에 올린다")
    void liftsDraftHeadline() {
        given(celebDraftClient.findDraft("카리나")).willReturn(json("""
                {"celebrity":"카리나","kind":"PERSON","status":"ok",
                 "shape":"매장 3 · 카페 1 · 여가 1","built_at":"2026-08-25T14:15:10",
                 "warnings":["사진 없음","근거 사진을 못 찾았다"],
                 "places":[{"place_name":"프라다"},{"place_name":"MLB"}]}
                """));

        AdminCourseDetailResponse response = service.getDraft("카리나");

        assertThat(response.getCelebrity()).isEqualTo("카리나");
        assertThat(response.getKind()).isEqualTo("PERSON");
        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getShape()).isEqualTo("매장 3 · 카페 1 · 여가 1");
        assertThat(response.getBuiltAt()).isEqualTo("2026-08-25T14:15:10");
        assertThat(response.getPlaceCount()).isEqualTo(2);
        assertThat(response.getWarningCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("코스를 못 만든 초안도 그대로 보여 준다 — 관리자가 사유를 봐야 한다")
    void showsFailedDraft() {
        given(celebDraftClient.findDraft("장원영")).willReturn(json("""
                {"celebrity":"장원영","status":"조사 빈손","why":"검색이 실패했거나 자료가 없다",
                 "warnings":["조사가 빈손이라 코스를 만들지 않았다"],"places":[]}
                """));

        AdminCourseDetailResponse response = service.getDraft("장원영");

        assertThat(response.getStatus()).isEqualTo("조사 빈손");
        assertThat(response.getPlaceCount()).isZero();
        assertThat(response.getWarningCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이름을 다듬어서 보낸다 — 앞뒤 공백이 붙은 이름은 초안을 못 찾는다")
    void trimsCelebrityName() {
        given(celebDraftClient.findDraft("카리나")).willReturn(json(
                "{\"celebrity\":\"카리나\",\"status\":\"ok\",\"places\":[],\"warnings\":[]}"));

        service.getDraft("  카리나  ");

        verify(celebDraftClient).findDraft("카리나");
    }

    @Test
    @DisplayName("빈 이름은 람다를 부르기 전에 400 으로 막는다")
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.getDraft("   "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        verify(celebDraftClient, never()).findDraft(anyString());
    }

    @Test
    @DisplayName("없는 초안은 404 다 — 그 창구의 오류는 뜻이 하나뿐이다")
    void missingDraftIsNotFound() {
        given(celebDraftClient.findDraft("없는사람")).willReturn(json(
                "{\"celebrity\":\"없는사람\",\"error\":\"초안이 없습니다 (만료됐거나 안 만들었다)\"}"));

        assertThatThrownBy(() -> service.getDraft("없는사람"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_NOT_FOUND);
    }

    @Test
    @DisplayName("실행 상황의 오류는 502 다 — Redis 에 못 붙은 것이지 없는 것이 아니다")
    void runErrorIsBadGateway() {
        given(celebDraftClient.findRunStatus()).willReturn(json(
                "{\"error\":\"Redis 에 못 붙었습니다\",\"redis_host\":\"ditto-celeb-cache\"}"));

        assertThatThrownBy(() -> service.getRunStatus())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_READ_FAILED);
    }

    @Test
    @DisplayName("목록의 오류도 502 다 — 초안이 없으면 빈 목록이 오지 오류가 오지 않는다")
    void listErrorIsBadGateway() {
        given(celebDraftClient.listDrafts()).willReturn(json(
                "{\"error\":\"Redis 에 못 붙었습니다\"}"));

        assertThatThrownBy(() -> service.getDrafts())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_READ_FAILED);
    }

    @Test
    @DisplayName("실행 상황은 대기 인원과 끝난 인원을 센다")
    void countsRunProgress() {
        given(celebDraftClient.findRunStatus()).willReturn(json("""
                {"date":"2026-08-25","queued":3,
                 "done":{"카리나":"ok · 매장 3 · 카페 1 · 여가 1","장원영":"실패 TimeoutError"}}
                """));

        AdminCourseRunResponse response = service.getRunStatus();

        assertThat(response.getDate()).isEqualTo("2026-08-25");
        assertThat(response.getQueued()).isEqualTo(3);
        assertThat(response.getDoneCount()).isEqualTo(2);
    }

    private JsonNode json(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
