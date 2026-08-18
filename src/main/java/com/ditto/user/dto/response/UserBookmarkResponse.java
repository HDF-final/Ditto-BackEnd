package com.ditto.user.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 북마크 코스 목록 응답 DTO")
public class UserBookmarkResponse {

    @Schema(description = "게시글 ID", example = "23")
    private Long postId;

    @Schema(description = "코스 ID", example = "23")
    private Long courseId;

    @Schema(description = "코스 제목", example = "K-POP 팝업스토어 & 한식 맛집 코스")
    private String title;

    @Schema(description = "좋아요 수", example = "78")
    private Long likeCount;

    @Schema(description = "북마크(저장) 수", example = "35")
    private Long bookmarkCount;

    @Schema(description = "북마크한 시각", example = "2026-08-18T14:20:00")
    private LocalDateTime bookmarkedAt;
}
