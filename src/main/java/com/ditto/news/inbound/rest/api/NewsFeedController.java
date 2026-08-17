package com.ditto.news.inbound.rest.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.response.NewsFeedDetailResponse;
import com.ditto.news.inbound.rest.dto.response.NewsFeedSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 사용자용 공개 뉴스피드 조회 REST API 컨트롤러 (Inbound Web Adapter).
 * 목록 페이징 조회 및 Slug/ID 상세 조회를 전담합니다.
 */
@Tag(name = "News", description = "K-컬처 트렌드 뉴스피드 조회 API")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsFeedController {

    private final NewsFeedService newsFeedService;

    @Operation(summary = "뉴스피드 목록 페이징 조회", description = "최신순으로 뉴스피드 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<NewsFeedSummaryResponse>> getNewsFeedList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        List<NewsFeed> feeds = newsFeedService.getNewsFeeds(page, size);

        List<NewsFeedSummaryResponse> response = feeds.stream()
                .map(NewsFeedSummaryResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "뉴스피드 상세 조회 (PK ID)", description = "ID로 뉴스피드 상세 본문을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<NewsFeedDetailResponse> getNewsFeedById(@PathVariable("id") Long id) {
        NewsFeed feed = newsFeedService.getNewsFeedById(id);
        return ApiResponse.success(NewsFeedDetailResponse.from(feed));
    }

    @Operation(summary = "뉴스피드 상세 조회 (URL Slug)", description = "고유 URL 슬러그로 뉴스피드 상세 본문을 조회합니다.")
    @GetMapping("/slug/{slug}")
    public ApiResponse<NewsFeedDetailResponse> getNewsFeedBySlug(@PathVariable("slug") String slug) {
        NewsFeed feed = newsFeedService.getNewsFeedBySlug(slug);
        return ApiResponse.success(NewsFeedDetailResponse.from(feed));
    }
}
