package com.ditto.course.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 코스 생성 API 응답. 명세: courseId, name, places[{ placeId, order }].
 */
@Getter
@AllArgsConstructor
public class CreateCourseResponse {

    private final Long courseId;
    private final String name;
    private final List<PlaceOrderResponse> places;

    @Getter
    @AllArgsConstructor
    public static class PlaceOrderResponse {
        private final Long placeId;
        private final int order;
    }
}
