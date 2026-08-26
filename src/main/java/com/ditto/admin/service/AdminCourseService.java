package com.ditto.admin.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ditto.admin.client.CelebApproveClient;
import com.ditto.admin.client.CelebDraftClient;
import com.ditto.admin.dto.response.AdminCourseApproveResponse;
import com.ditto.admin.dto.response.AdminCourseCacheListResponse;
import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCoursePlaceCatalogResponse;
import com.ditto.admin.dto.response.AdminCourseRevokeResponse;
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

    /**
     * 지금 손님에게 나가고 있는 코스 목록.
     *
     * <p>승인이 끝나면 그 인물은 초안 목록에서 사라진다 — 초안을 지우는 것이 승인의
     * 마지막 단계다. 관리자가 "올린 것이 지금 어떻게 나가나" 를 볼 자리가 여기다.
     *
     * <p>부르는 곳이 {@code CelebDraftClient} 가 아니라 승인 람다인 것은, 서빙 캐시
     * ({@code celeb:course:*})를 아는 쪽이 거기이기 때문이다 — 초안 람다는 그 키를
     * 읽는 코드가 아예 없다(그게 "초안은 손님에게 안 나간다" 의 보장이다).
     */
    public AdminCourseCacheListResponse getCachedCourses() {
        JsonNode payload = celebApproveClient.listCourses();
        // **빈 목록은 정상이다.** 그날 승인 전이면 아무것도 없는 것이 맞다. 람다는
        // Redis 에 못 붙었을 때만 {"error": …} 를 낸다 — 그 둘을 창구에서 갈라 뒀다.
        rejectError(payload, ErrorCode.CELEB_COURSE_CACHE_READ_FAILED);

        return AdminCourseCacheListResponse.builder()
                .functionName(celebApproveClient.getFunctionName())
                .fetchedAt(Instant.now())
                .count(payload.path("courses").size())
                .payload(payload)
                .build();
    }

    /**
     * 서비스 중인 코스 하나. <b>초안과 같은 칸으로 온다</b> — 화면이 두 가지 모양을
     * 알 이유가 없어, 응답 그릇도 초안 상세와 같은 것을 쓴다.
     *
     * <p>관리자가 이걸 고쳐 {@link #approve(String, JsonNode)} 에 다시 넣으면 덮어쓴다.
     * 승인이 멱등이라 따로 "수정" 창구를 두지 않는다.
     */
    public AdminCourseDetailResponse getCachedCourse(String celebrity, String aspect) {
        String name = celebrity == null ? "" : celebrity.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인물 이름이 비어 있습니다.");
        }
        String axis = aspect == null || aspect.isBlank() ? "BRAND" : aspect.trim().toUpperCase();

        JsonNode payload = celebApproveClient.findCourse(name, axis);
        // 이 창구의 오류는 "그런 코스가 없다"(만료됐거나 아직 승인 전) 아니면 Redis 장애다.
        // 둘을 여기서 가를 수 없다 — 목록 창구가 그때 502 를 내므로 거기서 갈린다.
        rejectError(payload, ErrorCode.CELEB_COURSE_CACHE_NOT_FOUND);

        return AdminCourseDetailResponse.builder()
                .functionName(celebApproveClient.getFunctionName())
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

    /**
     * 인물의 캐시를 통째로 내린다 — 코스(전 축) · 조사 재료 · 표기.
     *
     * <p><b>되돌리는 창구는 없다.</b> 다시 올리려면 배치를 돌려 초안을 새로 만들고
     * 승인한다. 화면이 두 번 눌러야 나가게 해 둔 것이 그 때문이다.
     *
     * <p>지운 것이 없어도(이미 만료됐다) 오류가 아니다 — 관리자가 원한 상태가 이미
     * 그것이고, 화면은 목록에서 카드가 사라지는 것으로 결과를 본다.
     */
    public AdminCourseRevokeResponse revoke(String celebrity) {
        String name = celebrity == null ? "" : celebrity.trim();
        if (name.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "인물 이름이 비어 있습니다.");
        }

        JsonNode payload = celebApproveClient.revoke(name);
        rejectError(payload, ErrorCode.CELEB_COURSE_REVOKE_FAILED);
        log.info("코스를 내렸다. celebrity={}, keys={}, aliases={}",
                name, payload.path("keys").asInt(), payload.path("aliases").asInt());

        return AdminCourseRevokeResponse.builder()
                .functionName(celebApproveClient.getFunctionName())
                .revokedAt(Instant.now())
                .celebrity(name)
                .keys(payload.path("keys").asInt())
                .aliases(payload.path("aliases").asInt())
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
        // **어느 람다를 불렀는지 코드로 안다.** 이름을 박아 두면 서비스 중인 코스가
        // 실패했는데 로그에는 초안 람다 이름이 찍힌다.
        log.warn("코스 창구가 오류를 돌려줬다. code={}, error={}",
                errorCode.getCode(), error.asText(""));
        throw new BusinessException(errorCode);
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
