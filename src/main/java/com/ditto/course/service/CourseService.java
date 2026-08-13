package com.ditto.course.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.domain.VisitStatus;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.CreateCourseResponse.PlaceOrderResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CourseInsertCommand;
import com.ditto.course.repository.CourseMapper.CoursePlaceInsertCommand;
import com.ditto.course.repository.PlaceMapper;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    static final String DEFAULT_COURSE_TITLE = "이름 없는 코스";
    private static final String SHARE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SHARE_CODE_LENGTH = 8;
    private static final int SHARE_CODE_MAX_RETRY = 5;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CourseMapper courseMapper;
    private final PlaceMapper placeMapper;
    private final SecureRandom secureRandom = new SecureRandom();

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

    /**
     * 내 코스를 생성한다. placeIds 가 비어 있으면 프론트 수동 모드의 빈 코스로 저장한다.
     * 장소는 {@code place} 테이블에 있는 ID 만 허용한다.
     */
    @Transactional
    public CreateCourseResponse create(Long userId, CreateCourseRequest request) {
        List<Long> placeIds = normalizePlaceIds(request.getPlaceIds());
        validatePlacesExist(placeIds);

        String name = resolveName(request.getName());
        String shareCode = generateUniqueShareCode();

        CourseInsertCommand command = new CourseInsertCommand();
        command.setUserId(userId);
        command.setSourceCourseId(null);
        command.setName(name);
        command.setDescription(trimToNull(request.getDescription()));
        command.setCreationType(CourseCreationType.MANUAL.name());
        command.setShareCode(shareCode);
        courseMapper.insert(command);

        insertPlaces(command.getCourseId(), placeIds);
        return new CreateCourseResponse(command.getCourseId(), name, toPlaceOrders(placeIds));
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

    private String generateUniqueShareCode() {
        for (int i = 0; i < SHARE_CODE_MAX_RETRY; i++) {
            String shareCode = randomShareCode();
            if (courseMapper.countByShareCode(shareCode) == 0) {
                return shareCode;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "코스 공유 코드를 생성하지 못했습니다.");
    }

    private String randomShareCode() {
        StringBuilder builder = new StringBuilder(SHARE_CODE_LENGTH);
        for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
            builder.append(SHARE_CODE_CHARS.charAt(secureRandom.nextInt(SHARE_CODE_CHARS.length())));
        }
        return builder.toString();
    }
}
