package com.ditto.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코스 게시글 북마크 응답 DTO")
public class BookmarkResponse {

    @Schema(description = "게시글 ID", example = "23")
    private Long postId;

    @Schema(description = "업데이트된 총 북마크(저장) 수", example = "35")
    private Integer bookmarkCount;

    @Schema(description = "현재 사용자의 북마크 여부", example = "true")
    private Boolean isBookmarked;
}
