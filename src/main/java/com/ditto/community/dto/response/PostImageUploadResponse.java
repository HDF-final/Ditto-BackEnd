package com.ditto.community.dto.response;

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
@Schema(description = "코스 게시글 사진 업로드 응답")
public class PostImageUploadResponse {

    @Schema(description = "사진을 첨부한 게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글에 첨부된 전체 사진 조회 URL 목록(정렬 순)")
    private List<String> imageUrls;
}
