package com.ditto.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 내 코스 목록의 한 항목. MyBatis 가 세터로 채운다.
 * 명세: { courseId, name, placeCount }
 */
@Getter
@Setter
@Schema(description = "내 코스 목록 항목")
public class MyCourseSummaryResponse {

    @Schema(description = "코스 ID", example = "100")
    private Long courseId;

    @Schema(description = "코스 이름", example = "나의 더현대 코스")
    private String name;

    @Schema(description = "코스에 담긴 장소 수", example = "3")
    private int placeCount;
}
