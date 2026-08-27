package com.ditto.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "커뮤니티 게시글 사진 응답")
public class PostImageResponse {

    @Schema(description = "게시글 사진 ID", example = "159")
    private final Long postImageId;

    @Schema(description = "게시글 사진 조회 URL")
    private final String imageUrl;

    @Schema(description = "사진 정렬 순서", example = "0")
    private final Integer sortOrder;
}
