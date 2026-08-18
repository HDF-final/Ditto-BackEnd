package com.ditto.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코스 게시글 댓글 수정 요청 DTO")
public class UpdateCommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다.")
    @Schema(description = "수정할 댓글 내용", example = "오후 2시 이후에 방문하시는 것을 추천드립니다.")
    private String content;
}
