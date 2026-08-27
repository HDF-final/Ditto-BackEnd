package com.ditto.course.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;

import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.domain.VisitStatus;
import com.ditto.course.dto.request.AddCoursePlaceRequest;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.AddCoursePlaceResponse;
import com.ditto.course.dto.response.CopyCourseResponse;
import com.ditto.course.dto.request.UpdateCourseRequest;
import com.ditto.course.dto.response.CourseDetailResponse;
import com.ditto.course.dto.response.CoursePlaceResponse;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.CreateCourseResponse.PlaceOrderResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.dto.response.UpdateCourseResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CourseInsertCommand;
import com.ditto.course.repository.CourseMapper.CoursePlaceInsertCommand;
import com.ditto.course.repository.PlaceMapper;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.ContentTranslationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    static final String DEFAULT_COURSE_TITLE = "이름 없는 코스";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CourseMapper courseMapper;
    private final PlaceMapper placeMapper;
    private final S3Provider s3Provider;
    private final ContentTranslationService contentTranslationService;

    /**
     * 코스 PK 조회. 58·59·60 에서 소유권 확인 전에 이 메서드를 쓴다.
     */
    public Course requireCourse(Long courseId) {
        return courseMapper.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    /**
     * 로그인 사용자의 코스 목록을 최신 생성순으로 페이징 조회한다.
     */
    public PageResponse<MyCourseSummaryResponse> getMyCourses(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        List<MyCourseSummaryResponse> content = courseMapper.findSummariesByUserId(userId, offset, safeSize);
        long totalElements = courseMapper.countByUserId(userId);
        return new PageResponse<>(content, safePage, totalElements);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<MyCourseSummaryResponse> getMyCourses(
            Long userId,
            int page,
            int size,
            ContentLanguage language) {
        PageResponse<MyCourseSummaryResponse> response = getMyCourses(userId, page, size);
        if (language == null || !language.requiresTranslation()) {
            return response;
        }
        for (MyCourseSummaryResponse course : response.getContent()) {
            course.setName(contentTranslationService.translate(
                    "course", String.valueOf(course.getCourseId()), "name", course.getName(), language));
        }
        return response;
    }

    /**
     * 조회 가능한 코스의 상세 정보와 방문 장소를 조회한다.
     * 조회 가능 조건은 본인 소유, SYSTEM 기본 코스, 유효한 공개 게시글 연결 코스다.
     */
    public CourseDetailResponse getDetail(Long userId, Long courseId) {
        Course course = requireCourse(courseId);
        validateReadableCourse(course, userId);
        return loadDetail(course);
    }

    private CourseDetailResponse loadDetail(Course course) {
        List<CoursePlaceResponse> places = courseMapper.findPlacesByCourseId(course.getCourseId());
        // DB에는 S3 object key가 저장되어 있으므로 클라이언트에는 조회용 URL로 변환해 내려준다.
        //
        // **`resolveImageUrl` 이 아니라 prefix 를 보는 쪽을 쓴다.** 자리 사진이 두 갈래로
        // 오기 때문이다 — 셀럽 사진은 `course/…` 라 CloudFront 에 동작이 없어 301 로
        // 튕기고, 매장 사진은 `place-picture/…` 라 CDN 으로 나가야 한다.
        for (CoursePlaceResponse place : places) {
            place.setImageUrl(s3Provider.resolveImageUrlByPrefix(place.getImageUrl()));
        }
        return CourseDetailResponse.from(
                course, places, s3Provider.resolveImageUrlByPrefix(
                        courseMapper.findHeroImageKey(course.getCourseId())));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CourseDetailResponse getDetail(Long userId, Long courseId, ContentLanguage language) {
        CourseDetailResponse response = getDetail(userId, courseId);
        if (language == null || !language.requiresTranslation()) {
            return response;
        }

        String courseKey = String.valueOf(response.getCourseId());
        for (CoursePlaceResponse place : response.getPlaces()) {
            String placeKey = String.valueOf(place.getPlaceId());
            place.setName(contentTranslationService.translate(
                    "place", placeKey, "name", place.getName(), language));
            place.setRecommendationReason(contentTranslationService.translate(
                    "course_place",
                    courseKey + ":" + placeKey,
                    "recommendation_reason",
                    place.getRecommendationReason(),
                    language));
        }
        return new CourseDetailResponse(
                response.getCourseId(),
                contentTranslationService.translate(
                        "course", courseKey, "name", response.getName(), language),
                contentTranslationService.translate(
                        "course", courseKey, "description", response.getDescription(), language),
                // 사진 주소는 번역하지 않는다 — 언어가 달라도 같은 코스의 같은 사진이다.
                response.getImageUrl(),
                response.getCreationType(),
                response.getSourceCourseId(),
                response.getCreatedAt(),
                response.getPlaces());
    }

    /**
     * 내 코스의 정보(이름·설명)와 방문 순서를 수정한다. 본인 코스만 가능하며,
     * orderedPlaceIds 는 코스에 속한 장소 전체를 바뀐 순서대로 담아야 한다(배열 순서 = visit_order).
     * PATCH 이므로 name 을 생략(null/공백)하면 기존 이름을, description 을 생략(null)하면 기존 설명을 유지한다.
     */
    @Transactional
    public UpdateCourseResponse update(Long userId, Long courseId, UpdateCourseRequest request) {
        Course course = requireCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
        }

        List<Long> orderedPlaceIds = normalizePlaceIds(request.getOrderedPlaceIds());
        validateReorderTargetsCourse(courseId, orderedPlaceIds);

        String name = StringUtils.hasText(request.getName())
                ? request.getName().trim()
                : course.getName();
        String description = (request.getDescription() != null)
                ? trimToNull(request.getDescription())
                : course.getDescription();
        courseMapper.updateInfo(courseId, name, description);
        if (!orderedPlaceIds.isEmpty()) {
            courseMapper.markVisitOrdersForReorder(courseId);
            courseMapper.reorderPlaces(courseId, orderedPlaceIds);
        }
        return new UpdateCourseResponse(courseId, name, orderedPlaceIds);
    }

    /** orderedPlaceIds 가 코스에 속한 장소 전체와 정확히 같은 집합인지 검증한다(추가·누락 금지). */
    private void validateReorderTargetsCourse(Long courseId, List<Long> orderedPlaceIds) {
        Set<Long> current = new HashSet<>(courseMapper.findPlaceIdsByCourseId(courseId));
        if (!current.equals(new HashSet<>(orderedPlaceIds))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "코스에 속한 장소 전체를 순서대로 전달해야 합니다.");
        }
    }

    /**
     * 내 코스를 삭제한다(soft delete). 본인 소유 코스만 삭제할 수 있다.
     */
    @Transactional
    public void delete(Long userId, Long courseId) {
        Course course = requireCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
        }
        courseMapper.softDelete(courseId);
    }

    /**
     * 내 코스를 생성한다. placeIds 가 비어 있으면 프론트 수동 모드의 빈 코스로 저장한다.
     * 장소는 {@code place} 테이블에 있는 ID 만 허용한다.
     */
    @Transactional
    public CreateCourseResponse create(Long userId, CreateCourseRequest request) {
        List<Long> placeIds = normalizePlaceIds(request.getPlaceIds());
        validatePlacesExist(placeIds);

        String name = resolveName(request.getName());

        CourseInsertCommand command = new CourseInsertCommand();
        command.setUserId(userId);
        command.setSourceCourseId(null);
        command.setName(name);
        command.setDescription(trimToNull(request.getDescription()));
        command.setCreationType(CourseCreationType.MANUAL.name());
        courseMapper.insert(command);

        insertPlaces(command.getCourseId(), placeIds);
        return new CreateCourseResponse(command.getCourseId(), name, toPlaceOrders(placeIds));
    }

    /**
     * 공개 원본 코스를 로그인 사용자의 내 코스로 복사한다.
     * 복사 가능 코스는 SYSTEM 기본 제공 코스 또는 유효한 공개 게시글에 연결된 코스다.
     */
    @Transactional
    public CopyCourseResponse copyPublicCourse(Long userId, Long sourceCourseId) {
        Course sourceCourse = requireCourse(sourceCourseId);
        validatePublicCourse(sourceCourse);

        String copiedName = sourceCourse.getName() + " Copy";
        CourseInsertCommand command = new CourseInsertCommand();
        command.setUserId(userId);
        command.setSourceCourseId(sourceCourseId);
        command.setName(copiedName);
        command.setDescription(sourceCourse.getDescription());
        command.setCreationType(CourseCreationType.COPIED.name());
        courseMapper.insert(command);

        courseMapper.copyPlacesFromCourse(sourceCourseId, command.getCourseId(), VisitStatus.PENDING.name());

        return CopyCourseResponse.builder()
                .sourceCourseId(sourceCourseId)
                .createdCourseId(command.getCourseId())
                .name(copiedName)
                .build();
    }

    /**
     * 내 코스의 지정된 순서에 장소를 추가한다.
     */
    @Transactional
    public AddCoursePlaceResponse addPlace(Long userId, Long courseId, AddCoursePlaceRequest request) {
        if (request == null || request.getPlaceId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장소 ID가 올바르지 않습니다.");
        }
        if (request.getPosition() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장소 순서가 올바르지 않습니다.");
        }

        Course course = requireCourse(courseId);
        validateCourseOwner(course, userId);
        validatePlacesExist(List.of(request.getPlaceId()));
        validatePlaceNotInCourse(courseId, request.getPlaceId());

        int position = request.getPosition();
        int maxVisitOrder = courseMapper.findMaxVisitOrder(courseId);
        validateInsertPosition(position, maxVisitOrder);

        if (position <= maxVisitOrder) {
            courseMapper.markVisitOrdersForShift(courseId, position);
            courseMapper.incrementMarkedVisitOrders(courseId);
        }

        courseMapper.insertPlace(new CoursePlaceInsertCommand(
                courseId,
                request.getPlaceId(),
                position,
                null,
                VisitStatus.PENDING.name()));

        return AddCoursePlaceResponse.builder()
                .courseId(courseId)
                .placeId(request.getPlaceId())
                .position(position)
                .build();
    }

    /**
     * 내 코스에서 장소를 삭제하고 뒤쪽 방문 순서를 한 칸씩 앞으로 당긴다.
     */
    @Transactional
    public void deletePlace(Long userId, Long courseId, Long placeId) {
        if (courseId == null || placeId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "요청을 처리할 수 없습니다.");
        }

        Course course = requireCourse(courseId);
        validateCourseOwner(course, userId);

        int deletedVisitOrder = courseMapper.findVisitOrderByCourseAndPlace(courseId, placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "요청을 처리할 수 없습니다."));

        courseMapper.deletePlace(courseId, placeId);
        courseMapper.markVisitOrdersAfterDeleted(courseId, deletedVisitOrder);
        courseMapper.decrementMarkedVisitOrders(courseId);
    }

    private List<Long> normalizePlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long placeId : placeIds) {
            if (placeId == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장소 ID가 올바르지 않습니다.");
            }
            if (!seen.add(placeId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_PLACE_IN_COURSE);
            }
            normalized.add(placeId);
        }
        return List.copyOf(normalized);
    }

    private void validatePlacesExist(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return;
        }
        Set<Long> existing = new LinkedHashSet<>(placeMapper.findExistingIds(placeIds));
        if (existing.size() != placeIds.size()) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
    }

    private void validateCourseOwner(Course course, Long userId) {
        if (!course.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
        }
    }

    private void validatePublicCourse(Course course) {
        if (CourseCreationType.SYSTEM.name().equals(course.getCreationType())) {
            return;
        }
        if (courseMapper.existsPublicPostByCourseId(course.getCourseId())) {
            return;
        }
        throw new BusinessException(ErrorCode.COURSE_NOT_PUBLIC);
    }

    private void validateReadableCourse(Course course, Long userId) {
        if (course.isOwnedBy(userId)) {
            return;
        }
        if (CourseCreationType.SYSTEM.name().equals(course.getCreationType())) {
            return;
        }
        if (courseMapper.existsPublicPostByCourseId(course.getCourseId())) {
            return;
        }
        throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
    }

    private void validatePlaceNotInCourse(Long courseId, Long placeId) {
        if (courseMapper.countPlaceInCourse(courseId, placeId) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_PLACE_IN_COURSE);
        }
    }

    private void validateInsertPosition(int position, int maxVisitOrder) {
        if (position > maxVisitOrder + 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장소 순서가 올바르지 않습니다.");
        }
    }

    private void insertPlaces(Long courseId, List<Long> placeIds) {
        for (int i = 0; i < placeIds.size(); i++) {
            courseMapper.insertPlace(new CoursePlaceInsertCommand(
                    courseId,
                    placeIds.get(i),
                    i + 1,
                    null,
                    VisitStatus.PENDING.name()));
        }
    }

    private List<PlaceOrderResponse> toPlaceOrders(List<Long> placeIds) {
        List<PlaceOrderResponse> places = new ArrayList<>();
        for (int i = 0; i < placeIds.size(); i++) {
            places.add(new PlaceOrderResponse(placeIds.get(i), i + 1));
        }
        return places;
    }

    private String resolveName(String name) {
        if (!StringUtils.hasText(name)) {
            return DEFAULT_COURSE_TITLE;
        }
        return name.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
