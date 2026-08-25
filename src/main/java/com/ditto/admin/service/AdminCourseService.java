package com.ditto.admin.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ditto.admin.client.CelebApproveClient;
import com.ditto.admin.client.CelebDraftClient;
import com.ditto.admin.dto.response.AdminCourseApproveResponse;
import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCoursePlaceCatalogResponse;
import com.ditto.admin.dto.response.AdminCourseRunResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code ditto-celeb-warm-2} 가 만든 승인 대기 코스 초안을 읽고, 관리자가 확정한 것을
 * {@code ditto-celeb-approve} 로 넘긴다.
 *
 * <p><b>이 백엔드가 직접 Redis 나 Oracle 을 건드리지는 않는다.</b> 캐시 키의 규칙과
 * 쓰는 순서는 람다가 안다 — 여기서 흉내 내면 두 곳이 같은 규칙을 따로 들게 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseService {

    private final CelebDraftClient celebDraftClient;
    private final CelebApproveClient celebApproveClient;

    public AdminCourseListResponse getDrafts() {
        JsonNode payload = celebDraftClient.listDrafts();
        // 이 창구는 초안이 없으면 빈 목록을 준다. 오류가 왔다면 Redis 쪽 문제다.
        rejectError(payload, ErrorCode.CELEB_DRAFT_READ_FAILED);

        return AdminCourseListResponse.builder()
                .functionName(celebDraftClient.getFunctionName())
                .fetchedAt(Instant.now())
                .count(payload.path("drafts").size())
                .payload(payload)
                .build();
    }

    public AdminCourseDetailResponse getDraft(String celebrity) {
        String name = celebrity == null ? "" : celebrity.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인물 이름이 비어 있습니다.");
        }

        JsonNode payload = celebDraftClient.findDraft(name);
        // 이 창구의 오류는 한 가지 뜻이다 — 그런 초안이 없다(만료됐거나 아직 안 만들었다).
        //
        // Redis 에 못 붙어도 같은 문구가 오므로 둘을 여기서 가를 수 없다. 화면이 초안
        // 전체가 안 보인다고 하면 /run 을 보면 된다 — 그쪽은 Redis 실패를 따로 말한다.
        rejectError(payload, ErrorCode.CELEB_DRAFT_NOT_FOUND);

        return AdminCourseDetailResponse.builder()
                .functionName(celebDraftClient.getFunctionName())
                .fetchedAt(Instant.now())
                .celebrity(payload.path("celebrity").asText(name))
                .kind(textOrNull(payload, "kind"))
                .status(textOrNull(payload, "status"))
                .shape(textOrNull(payload, "shape"))
                .builtAt(textOrNull(payload, "built_at"))
                .placeCount(payload.path("places").size())
                .warningCount(payload.path("warnings").size())
                .payload(payload)
                .build();
    }

    public AdminCourseRunResponse getRunStatus() {
        JsonNode payload = celebDraftClient.findRunStatus();
        rejectError(payload, ErrorCode.CELEB_DRAFT_READ_FAILED);

        return AdminCourseRunResponse.builder()
                .functionName(celebDraftClient.getFunctionName())
                .fetchedAt(Instant.now())
                .date(textOrNull(payload, "date"))
                .queued(payload.path("queued").asInt())
                .doneCount(payload.path("done").size())
                .payload(payload)
                .build();
    }

    /**
     * 더현대 장소 전부. 관리자가 초안의 자리를 갈아 끼울 때 고를 재료다.
     *
     * <p>람다는 조회에 실패해도 오류가 아니라 <b>빈 목록</b>을 준다 — 목록을 못 얻었다고
     * 초안 만들기까지 같이 죽으면 안 된다는 판단이다. 그래서 여기서도 빈 목록이 정상 응답이고,
     * 화면이 "고를 것이 없다"로 표시한다.
     */
    public AdminCoursePlaceCatalogResponse getPlaces(boolean fresh) {
        JsonNode payload = celebDraftClient.findPlaces(fresh);
        rejectError(payload, ErrorCode.CELEB_DRAFT_READ_FAILED);

        return AdminCoursePlaceCatalogResponse.builder()
                .functionName(celebDraftClient.getFunctionName())
                .fetchedAt(Instant.now())
                .count(payload.path("places").size())
                .payload(payload)
                .build();
    }

    /**
     * 관리자가 고친 초안을 승인해 손님 캐시로 올린다.
     *
     * <p><b>경로의 인물과 본문의 인물이 다르면 막는다.</b> 화면이 다른 초안을 열어 둔
     * 채로 요청이 나가면 남의 인물 캐시를 덮어쓰게 된다 — 그건 하루 종일 손님에게
     * 나가는 값이라 되돌릴 창구가 없다.
     */
    public AdminCourseApproveResponse approve(String celebrity, JsonNode draft) {
        String name = celebrity == null ? "" : celebrity.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인물 이름이 비어 있습니다.");
        }
        if (draft == null || !draft.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "초안 본문이 비어 있습니다.");
        }
        String inBody = draft.path("celebrity").asText("").trim();
        if (!name.equals(inBody)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "경로의 인물(" + name + ")과 본문의 인물(" + inBody + ")이 다릅니다.");
        }

        JsonNode payload = celebApproveClient.approve(draft);

        // 람다는 검증 실패를 예외가 아니라 {"ok":false,"errors":[…]} 로 돌려준다.
        // 이걸 놓치면 아무것도 안 올라갔는데 화면이 "승인했습니다" 라고 한다.
        if (!payload.path("ok").asBoolean(false)) {
            String why = payload.path("errors").toString();
            log.warn("승인이 거절됐다. celebrity={}, errors={}", name, why);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "승인할 수 없습니다: " + why);
        }

        return AdminCourseApproveResponse.builder()
                .functionName(celebApproveClient.getFunctionName())
                .approvedAt(Instant.now())
                .celebrity(payload.path("celebrity").asText(name))
                .placeCount(payload.path("places").asInt())
                .expiresAt(textOrNull(payload, "expires_at"))
                .warnings(payload.get("warnings"))
                .payload(payload)
                .build();
    }

    /**
     * 람다는 실패를 예외로 올리지 않고 {@code {"error": "…"}} 를 정상 응답으로 돌려준다.
     * 이걸 놓치면 칸이 전부 비어 있는 초안이 {@code success:true} 로 관리자 화면에 뜬다.
     */
    private void rejectError(JsonNode payload, ErrorCode errorCode) {
        JsonNode error = payload.path("error");
        if (error.isMissingNode() || error.isNull()) {
            return;
        }
        log.warn("코스 초안 창구가 오류를 돌려줬다. functionName={}, code={}, error={}",
                celebDraftClient.getFunctionName(), errorCode.getCode(), error.asText(""));
        throw new BusinessException(errorCode);
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
