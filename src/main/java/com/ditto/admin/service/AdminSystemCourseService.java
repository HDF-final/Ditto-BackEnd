package com.ditto.admin.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.admin.client.CelebApproveClient;
import com.ditto.admin.dto.request.AdminSystemCourseUpdateRequest;
import com.ditto.admin.dto.response.AdminSystemCoursePlaceResponse;
import com.ditto.admin.dto.response.AdminSystemCourseResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.recommendation.repository.RecommendedCourseMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

/**
 * 어드민 "기본 추천 코스" — 목록 · 수정 · 삭제.
 *
 * <h2>목록의 원본은 오라클이다</h2>
 * 진행 상태는 AI 람다(Redis)에 있지만 <b>목록을 거기서 만들지 않는다.</b> 코스는 영구고
 * 상태는 부가 정보라, 상태를 원본으로 삼으면 람다나 Redis 가 죽었을 때 관리자 화면에
 * "기본 추천 코스가 하나도 없다" 가 뜬다 — 실제로는 다 걸려 있는데.
 *
 * <p>그래서 상태 조회는 <b>실패해도 삼킨다.</b> 인물 이름과 진행 문구가 빠질 뿐이다.
 */
@Service
@RequiredArgsConstructor
public class AdminSystemCourseService {

    private static final Logger log = LoggerFactory.getLogger(AdminSystemCourseService.class);

    /** 반영 기록이 없는 코스의 상태. 이 창구가 생기기 전에 올린 것이 여기 해당한다. */
    private static final String STATE_DONE = "done";
    private static final String STEP_DONE = "반영됨";

    /**
     * DB 시계의 시간대. {@code course.created_at} 은 {@code SYSTIMESTAMP} 로 들어가는데
     * 그 서버가 <b>UTC</b> 로 돌고 있다.
     *
     * <p>실측이다 — 반영 람다(TZ=Asia/Seoul)가 같은 순간에 적는 값과 정확히 아홉 시간
     * 차이가 났다(코스 189: 오라클 07:00:02 / 람다 16:00:02).
     */
    private static final ZoneOffset DB_ZONE = ZoneOffset.UTC;

    private final RecommendedCourseMapper mapper;
    private final CelebApproveClient celebApproveClient;
    private final S3Provider s3Provider;

    @Transactional(readOnly = true)
    public List<AdminSystemCourseResponse> getCourses() {
        Map<Long, JsonNode> states = publishStates();
        return mapper.findAllForAdmin().stream()
                .map(row -> toResponse(row, states.get(row.getCourseId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSystemCourseResponse getCourse(Long courseId) {
        RecommendedCourseMapper.AdminCourseRow row = mapper.findOneForAdmin(courseId);
        if (row == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        List<AdminSystemCoursePlaceResponse> places = mapper.findPlaces(courseId).stream()
                .map(p -> AdminSystemCoursePlaceResponse.builder()
                        .placeId(p.getPlaceId())
                        .name(p.getName())
                        // DB 에는 키만 들어 있다. 화면이 그대로 <img src> 로 쓰게 만든다.
                        //
                        // **`resolveImageUrl` 이 아니라 prefix 를 보는 쪽이다.** 이 자리에
                        // 셀럽 사진(`course/…`)이 오게 되면서 갈림길이 생겼다 — CloudFront 에
                        // 그 prefix 동작이 없어 CDN 주소로 만들면 301 로 튕긴다.
                        .imageUrl(s3Provider.resolveImageUrlByPrefix(p.getImageUrl()))
                        .imageKey(p.getImageUrl())
                        .floorCode(p.getFloorCode())
                        .visitOrder(p.getVisitOrder())
                        .recommendationReason(p.getRecommendationReason())
                        .build())
                .toList();
        return toResponse(row, publishStates().get(courseId), places);
    }

    /**
     * 보낸 칸만 고친다. null 은 "그대로 둬라" 다.
     *
     * <p>매퍼가 SQL 조건에 {@code creation_type = 'SYSTEM'} 을 걸어 둬서, 관리자가 주소창의
     * 코스 번호를 바꿔 손님 코스를 고치는 길이 막혀 있다. 여기서 한 번 더 확인하는 것은
     * 없는 번호와 "SYSTEM 이 아닌 번호" 를 같은 404 로 돌려주기 위해서다.
     */
    @Transactional
    public AdminSystemCourseResponse updateCourse(Long courseId,
                                                  AdminSystemCourseUpdateRequest request) {
        RecommendedCourseMapper.AdminCourseRow row = mapper.findOneForAdmin(courseId);
        if (row == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        String name = request.getName() == null ? row.getName() : request.getName().trim();
        if (name.isBlank()) {
            // COURSE.NAME 은 NOT NULL 이다. 빈 이름을 넣으면 목록 카드가 제목 없이 그려진다.
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String description = request.getDescription() == null
                ? row.getDescription() : request.getDescription();

        mapper.updateInfo(courseId, name, description);

        // 빈 배열은 "나라를 전부 뗀다" 로 읽는다 — null("그대로 둬라")과 뜻이 다르다.
        // 나라가 없는 코스는 손님 화면의 어느 나라 버튼에서도 안 보이니 사실상 내리는 것이다.
        if (request.getCountryCodes() != null) {
            mapper.deleteCountries(courseId);
            for (String code : normalizeCountries(request.getCountryCodes())) {
                mapper.insertCountry(courseId, code);
            }
        }

        // **빈 문자열이 "기본값으로 되돌린다" 는 뜻이다.** null 은 여기서도 "그대로 둬라" 다.
        if (request.getMainImage() != null) {
            String key = request.getMainImage().trim();
            mapper.updateMainImage(courseId, key.isEmpty() ? null : key);
        }

        if (request.getPostContent() != null && row.getPostId() != null) {
            mapper.updatePostContent(courseId, request.getPostContent());
        }
        if (request.getPlaces() != null) {
            for (AdminSystemCourseUpdateRequest.PlaceReason place : request.getPlaces()) {
                if (place.getPlaceId() == null || place.getRecommendationReason() == null) {
                    continue;
                }
                // 빈 꼬리표를 막는다. 화면이 장소 이름 밑에 한 줄을 그리는 자리라,
                // 비면 카드가 자리마다 다른 높이가 된다.
                String reason = place.getRecommendationReason().trim();
                if (reason.isBlank()) {
                    continue;
                }
                mapper.updatePlaceReason(courseId, place.getPlaceId(), reason);
            }
        }
        return getCourse(courseId);
    }

    /**
     * 내린다. 코스와 붙어 있던 게시글을 같이 지운다(soft delete).
     *
     * <p><b>복사해 간 손님 코스는 안 건드린다.</b> 그건 COPIED 로 만들어진 자기 행이고,
     * 원본을 내렸다고 남의 코스를 지울 일이 아니다.
     *
     * <p>AI 쪽 캐시(즉답용)도 안 건드린다. 그건 자정에 사라지고, 여기서 같이 내리면
     * "메인에서만 빼려던" 관리자가 오늘 즉답까지 끄게 된다.
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        if (mapper.findOneForAdmin(courseId) == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        mapper.softDeletePost(courseId);
        mapper.softDelete(courseId);
    }

    /** course_id → 반영 상태. 실패하면 빈 표 — 목록은 그려져야 한다. */
    private Map<Long, JsonNode> publishStates() {
        Map<Long, JsonNode> out = new HashMap<>();
        try {
            JsonNode got = celebApproveClient.listPublished();
            JsonNode courses = got.path("courses");
            if (courses.isArray()) {
                for (JsonNode node : courses) {
                    JsonNode id = node.path("course_id");
                    if (id.isNumber()) {
                        out.put(id.asLong(), node);
                    }
                }
            }
        } catch (Exception e) {
            // 상태는 부가 정보다. 이것 때문에 관리자 화면이 죽으면 안 된다.
            log.warn("[AdminSystemCourse] 반영 상태를 못 읽었다: {}", e.getMessage());
        }
        return out;
    }

    private AdminSystemCourseResponse toResponse(RecommendedCourseMapper.AdminCourseRow row,
                                                 JsonNode state,
                                                 List<AdminSystemCoursePlaceResponse> places) {
        List<String> warnings = new ArrayList<>();
        if (state != null && state.path("warnings").isArray()) {
            state.path("warnings").forEach(w -> warnings.add(w.asText()));
        }
        return AdminSystemCourseResponse.builder()
                .courseId(row.getCourseId())
                .name(row.getName())
                .description(row.getDescription())
                .countryCodes(splitCountries(row.getCountryCodes()))
                .mainImage(row.getMainImage())
                .mainImageUrl(heroUrl(row.getMainImage()))
                .shareCode(row.getShareCode())
                .placeCount(row.getPlaceCount())
                .postId(row.getPostId())
                .postContent(row.getPostContent())
                .imageCount(row.getImageCount())
                // DB 에는 키만 들어 있다. 자리가 없는 코스는 키도 없어 null 로 나가고,
                // 그때 카드가 사진 자리를 이름으로 채운다.
                .heroImageUrl(heroUrl(row.getHeroImageKey()))
                .createdAt(withZone(row.getCreatedAt()))
                .updatedAt(withZone(row.getUpdatedAt()))
                .celebrity(text(state, "celebrity", null))
                .state(text(state, "state", STATE_DONE))
                .step(text(state, "step", STEP_DONE))
                .error(text(state, "error", null))
                .warnings(warnings)
                .places(places)
                .build();
    }

    /**
     * {@code 'KR,JP'} → {@code [KR, JP]}. 값이 없으면 빈 목록이다 — null 을 주면
     * 화면이 칩을 그리기 전에 매번 null 을 가려야 한다.
     */
    private static List<String> splitCountries(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    /** 보낸 나라 목록을 {@code COUNTRY.CODE} 모양으로 고른다. 중복·빈 값은 뺀다. */
    private static List<String> normalizeCountries(List<String> codes) {
        return new ArrayList<>(new LinkedHashSet<>(
                codes.stream()
                        .filter(code -> code != null && !code.isBlank())
                        .map(code -> code.trim().toUpperCase(java.util.Locale.ROOT))
                        .toList()));
    }

    /**
     * 대표 사진 키 → 주소. 셀럽 사진만 CDN 을 건너뛴다.
     *
     * <p>판단은 {@link com.ditto.global.infrastructure.s3.S3Provider#resolveImageUrlByPrefix}
     * 로 옮겼다. 같은 규칙이 어드민·손님 목록·코스 상세 세 곳에 손으로 적혀 있었는데,
     * 이제 자리 사진까지 같은 갈림길을 지난다 — 한 군데만 고쳐지는 날이 오기 전에 모았다.
     */
    private String heroUrl(String key) {
        return s3Provider.resolveImageUrlByPrefix(key);
    }

    /**
     * 칸 없는 DB 시각에 시간대를 붙인다. <b>시각을 옮기지 않는다</b> — 같은 순간을
     * 가리키는 채로 어느 시간대의 값인지만 밝힌다.
     */
    private static OffsetDateTime withZone(LocalDateTime value) {
        return value == null ? null : value.atOffset(DB_ZONE);
    }

    private static String text(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : fallback;
    }
}
