package com.ditto.course.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내 코스 수정 응답. 명세: { courseId, name, orderedPlaceIds }
 */
@Getter
@AllArgsConstructor
@Schema(description = "내 코스 수정 응답")
public class UpdateCourseResponse {

    @Schema(description = "코스 ID", example = "100")
    private final Long courseId;

    @Schema(description = "수정된 코스 이름", example = "수정된 코스명")
    private final String name;

    @Schema(description = "적용된 방문 순서(장소 ID 목록)", example = "[33, 11, 22]")
    private final List<Long> orderedPlaceIds;
}
