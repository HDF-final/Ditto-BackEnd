package com.ditto.course.dto.response;

import java.util.List;
import java.time.LocalDateTime;

import com.ditto.course.domain.Course;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 코스 상세 공통 응답. 생성·조회·장소 추가·복사 후 응답에 재사용한다.
 */
@Getter
@AllArgsConstructor
public class CourseDetailResponse {

    private final Long courseId;
    private final String name;
    private final String description;
    private final String creationType;
    private final Long sourceCourseId;
    private final String shareCode;
    private final LocalDateTime createdAt;
    private final List<CoursePlaceResponse> places;

    public static CourseDetailResponse from(Course course, List<CoursePlaceResponse> places) {
        List<CoursePlaceResponse> safePlaces = places == null ? List.of() : places;
        return new CourseDetailResponse(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCreationType(),
                course.getSourceCourseId(),
                course.getShareCode(),
                course.getCreatedAt(),
                safePlaces);
    }
}
