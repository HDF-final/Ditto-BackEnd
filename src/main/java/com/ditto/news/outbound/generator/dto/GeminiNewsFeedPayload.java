package com.ditto.news.outbound.generator.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Gemini LLM이 생성한 구조화된 뉴스피드 JSON 페이로드 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiNewsFeedPayload {

    /** AI가 재작성한 헤드라인 제목 */
    private String title;

    /** 3줄 핵심 요약 리스트 */
    private List<String> summaries;

    /** 2차 가공된 본문 텍스트 */
    private String body;

    /** 해시태그/키워드 목록 */
    private List<String> keywords;
}
