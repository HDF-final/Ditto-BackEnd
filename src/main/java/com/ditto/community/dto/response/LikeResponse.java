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
@Schema(description = "코스 게시글 좋아요 응답 DTO")
public class LikeResponse {

    @Schema(description = "게시글 ID", example = "23")
    private Long postId;

    @Schema(description = "업데이트된 총 좋아요 수", example = "78")
    private Integer likesCount;

    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    private Boolean isLiked;
}
