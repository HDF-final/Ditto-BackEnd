package com.ditto.news.inbound.rest.dto.response;

import java.util.List;

import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스피드 파이프라인 수동 디버깅 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "뉴스피드 파이프라인 디버깅 응답")
public class NewsPipelineDebugResponse {

    @Schema(description = "요청 토픽", example = "K-POP")
    private String topic;

    @Schema(description = "1단계: RSS에서 수집된 후보 기사 수", example = "15")
    private int candidateCount;

    @Schema(description = "2단계: Python 크롤러가 본문 수집에 성공한 기사 수", example = "12")
    private int crawledCount;

    @Schema(description = "3단계: K-컬처 가중치로 최종 선별된 기사 수", example = "5")
    private int selectedCount;

    @Schema(description = "3단계: 최종 선별된 기사 상세 목록")
    private List<CrawledNewsArticle> selectedArticles;

    @Schema(description = "4단계: 단일/대표 생성된 뉴스피드 카드")
    private GeneratedNewsFeed generatedFeed;

    @Schema(description = "4단계: 개별 생성된 다건 뉴스피드 카드 목록")
    private List<GeneratedNewsFeed> generatedFeeds;

    @Schema(description = "5단계: DB 저장 후 발급된 대표 PK ID", example = "101")
    private Long savedNewsFeedId;

    @Schema(description = "5단계: DB 저장 후 발급된 PK ID 목록")
    private List<Long> savedNewsFeedIds;
}
