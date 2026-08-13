package com.ditto.course.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내 코스 정보·방문 순서 수정 요청.
 * orderedPlaceIds 는 코스에 속한 장소 전체를 바뀐 순서대로 담는다(배열 순서 = visit_order).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 코스 수정 요청")
public class UpdateCourseRequest {

    @Size(max = 100)
    @Schema(description = "수정할 코스 이름", example = "수정된 코스명")
    private String name;

    @Size(max = 500)
    @Schema(description = "수정할 코스 설명", example = "설명 수정")
    private String description;

    @Schema(description = "바뀐 순서대로의 장소 ID 목록(배열 순서 = 방문 순서)", example = "[33, 11, 22]")
    private List<Long> orderedPlaceIds;
}
