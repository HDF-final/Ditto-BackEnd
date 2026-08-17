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
@Schema(description = "댓글 작성 요청")
public class CreateCommentRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다.")
    @Schema(description = "댓글 내용", example = "오전에 가려면 몇 시쯤 도착하는 게 좋을까요?")
    private String content;
}
