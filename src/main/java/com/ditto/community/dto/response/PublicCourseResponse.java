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

    @Schema(description = "작성자 ID", example = "1")
    private Long writerId;

    @Schema(description = "작성자 닉네임", example = "구본희")
    private String writerNickname;

    @Schema(description = "작성자 국가 코드", example = "KR")
    private String country;

    @Schema(description = "게시글 제목", example = "내가 다녀온 K-MZ 코스")
    private String title;

    @Schema(description = "게시글 사진 조회 URL 목록")
    private List<String> imageUrls;

    @Schema(description = "좋아요 수", example = "12")
    private Long likeCount;

    @Schema(description = "북마크(저장) 수", example = "4")
    private Long bookmarkCount;
}
