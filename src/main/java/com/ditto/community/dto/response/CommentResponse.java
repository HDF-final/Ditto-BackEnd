package com.ditto.community.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
@Schema(description = "댓글 응답")
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long commentId;

    @Schema(description = "게시글 ID", example = "10")
    private Long postId;

    @Schema(description = "작성자 ID", example = "2")
    private Long userId;

    @Schema(description = "작성자 닉네임", example = "Chen_Li")
    private String nickname;

    @Schema(description = "게시글 작성자 여부 (작성자 뱃지 표시용)", example = "false")
    private Boolean isAuthor;

    @Schema(description = "댓글 내용", example = "오전에 가려면 몇 시쯤 도착하는 게 좋을까요?")
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "작성 일시", example = "2026-08-17T14:26:00")
    private LocalDateTime createdAt;
}
