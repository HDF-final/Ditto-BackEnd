package com.ditto.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "추천 코스 저장 응답 DTO")
public class CourseBookmarkResponse {

    @Schema(description = "코스 ID", example = "188")
    private Long courseId;

    @Schema(description = "현재 사용자의 저장 여부", example = "true")
    private Boolean isBookmarked;
}
