package com.ditto.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 공개 코스 복사 응답.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "공개 코스 복사 응답")
public class CopyCourseResponse {

    @Schema(description = "원본 코스 ID", example = "3")
    private final Long sourceCourseId;

    @Schema(description = "새로 생성된 내 코스 ID", example = "101")
    private final Long createdCourseId;

    @Schema(description = "복사된 코스 이름", example = "K-MZ Course Copy")
    private final String name;
}
