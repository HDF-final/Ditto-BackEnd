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
@Schema(description = "공개 코스 게시글 목록 항목 응답")
public class PublicCourseResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "연결된 코스 ID", example = "3")
    private Long courseId;

    @Schema(description = "게시글 제목", example = "내가 다녀온 K-MZ 코스")
    private String title;

    @Schema(description = "작성자 닉네임", example = "Yuki_T")
    private String writerNickname;

    @Schema(description = "좋아요 수", example = "12")
    private Long likeCount;

    @Schema(description = "북마크(저장) 수", example = "4")
    private Long bookmarkCount;

    @Schema(description = "댓글 수", example = "2")
    private Long commentCount;

    @Schema(description = "게시글에 첨부된 사진 조회 URL 목록(정렬 순, 없으면 빈 배열)")
    private List<String> imageUrls;
}
