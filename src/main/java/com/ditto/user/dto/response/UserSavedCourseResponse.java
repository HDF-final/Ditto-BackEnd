package com.ditto.user.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 저장한 추천 코스 목록 응답 DTO")
public class UserSavedCourseResponse {

    @Schema(description = "코스 ID", example = "188")
    private Long courseId;

    @Schema(description = "코스 제목", example = "에스파 브랜드 코스")
    private String title;

    @Schema(description = "코스 설명", example = "에스파 멤버들이 앰버서더로 활동하는 브랜드 중심 코스입니다.")
    private String description;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.example.com/course/...")
    private String imageUrl;

    @Schema(description = "코스 장소 수", example = "5")
    private Integer placeCount;

    @Schema(description = "저장한 시각", example = "2026-08-31T00:22:00")
    private LocalDateTime bookmarkedAt;
}
