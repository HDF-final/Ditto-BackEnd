package com.ditto.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내 코스에 장소를 추가하는 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCoursePlaceRequest {

    @NotNull
    @Positive
    private Long placeId;

    @NotNull
    @Positive
    private Integer position;
}
