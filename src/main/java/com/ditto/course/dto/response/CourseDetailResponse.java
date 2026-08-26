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

    /**
     * 코스 머리에 그리는 대표 사진. <b>바로 쓸 수 있는 주소</b>다.
     *
     * <p>관리자가 지정한 것 → 셀럽 사진 → 첫 자리 매장 사진 차례로 고른다
     * ({@code CourseMapper.findHeroImageKey}). 기본 추천 코스 목록이 쓰는 것과 같은
     * 규칙이라 카드와 상세가 같은 사진을 단다.
     *
     * <p>사진이 하나도 없으면 {@code null} 이고, 그때 화면이 기본 이미지로 떨어진다.
     */
    private final String imageUrl;
    private final String creationType;
    private final Long sourceCourseId;
    private final LocalDateTime createdAt;
    private final List<CoursePlaceResponse> places;

    public static CourseDetailResponse from(
            Course course, List<CoursePlaceResponse> places, String imageUrl) {
        List<CoursePlaceResponse> safePlaces = places == null ? List.of() : places;
        return new CourseDetailResponse(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                imageUrl,
                course.getCreationType(),
                course.getSourceCourseId(),
                course.getCreatedAt(),
                safePlaces);
    }
}
