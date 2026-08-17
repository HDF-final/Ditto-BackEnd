package com.ditto.news.inbound.rest.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스피드 수정 요청 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "뉴스피드 수정 요청")
public class NewsFeedUpdateRequest {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Schema(description = "수정할 뉴스피드 제목", example = "K-POP 글로벌 차트 돌풍")
    private String title;

    @NotBlank(message = "본문은 필수 입력 항목입니다.")
    @Schema(description = "수정할 뉴스피드 본문", example = "수정된 뉴스 본문 내용...")
    private String body;

    @Schema(description = "수정할 3줄 요약 리스트")
    private List<String> summaries;

    @Schema(description = "수정할 키워드/태그 목록")
    private List<String> keywords;

    @Schema(description = "수정할 대표 이미지 URL", example = "https://img.yna.co.kr/new-photo.jpg")
    private String representativeImageUrl;
}
