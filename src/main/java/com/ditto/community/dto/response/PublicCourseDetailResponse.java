package com.ditto.community.dto.response;

import java.time.LocalDateTime;
import java.util.List;

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
@Schema(description = "공개 코스 게시글 상세 응답")
public class PublicCourseDetailResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글 제목", example = "내가 다녀온 K-MZ 코스")
    private String title;

    @Schema(description = "게시글 본문", example = "추천 동선입니다.")
    private String content;

    @Schema(description = "좋아요 수", example = "12")
    private Long likeCount;

    @Schema(description = "북마크(저장) 수", example = "4")
    private Long bookmarkCount;

    @Schema(description = "게시글 생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "게시글에 첨부된 사진 조회 URL 목록(정렬 순, 없으면 빈 배열)")
    private List<String> imageUrls;

    @Schema(description = "게시글 사진 목록(ID 포함, 정렬 순, 없으면 빈 배열)")
    private List<PostImageResponse> images;

    @Schema(description = "연결된 코스 정보")
    private CourseInfo course;

    @Schema(description = "댓글 목록")
    private List<CommentResponse> comments;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코스 상세 정보")
    public static class CourseInfo {
        @Schema(description = "코스 ID", example = "3")
        private Long courseId;

        @Schema(description = "코스에 포함된 장소 목록")
        private List<PlaceInfo> places;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "코스 장소 순서 정보")
    public static class PlaceInfo {
        @Schema(description = "장소 ID", example = "11")
        private Long placeId;

        @Schema(description = "장소명", example = "탬버린즈")
        private String name;

        @Schema(description = "층 정보", example = "1F")
        private String floor;

        @Schema(description = "방문 순서", example = "1")
        private Integer order;
    }
}
