package com.ditto.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 내 코스 장소 추가 응답.
 */
@Getter
@Builder
@AllArgsConstructor
public class AddCoursePlaceResponse {

    private final Long courseId;
    private final Long placeId;
    private final int position;
}
