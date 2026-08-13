package com.ditto.global.common.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 목록 조회 공통 페이지 응답. {@code data} 안에 담아 재사용한다.
 * 명세: { content: [...], page, totalElements }
 */
@Getter
@AllArgsConstructor
@Schema(description = "페이지 목록 응답")
public class PageResponse<T> {

    @Schema(description = "현재 페이지 항목 목록")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
    private final int page;

    @Schema(description = "조건에 해당하는 전체 항목 수", example = "1")
    private final long totalElements;
}
