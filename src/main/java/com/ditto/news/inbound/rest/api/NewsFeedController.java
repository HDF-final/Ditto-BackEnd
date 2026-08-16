package com.ditto.news.inbound.rest.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.ditto.news.inbound.rest.dto.response.NewsFeedDetailResponse;
import com.ditto.news.inbound.rest.dto.response.NewsFeedSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 및 관리자용 뉴스피드 조회/수정/삭제 REST API 컨트롤러 (Inbound Web Adapter).
 * HTTP 요청/응답 DTO와 Application Domain 간의 변환을 전담합니다.
 */
@Tag(name = "News", description = "K-컬처 트렌드 뉴스피드 API")
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

    @Operation(summary = "뉴스피드 수정", description = "뉴스피드 제목, 본문, 요약, 태그를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<NewsFeedDetailResponse> updateNewsFeed(
            @PathVariable("id") Long id,
            @Valid @RequestBody NewsFeedUpdateRequest request) {
        NewsFeed updated = newsFeedService.updateNewsFeed(
                id,
                request.getTitle(),
                request.getBody(),
                request.getRepresentativeImageUrl(),
                request.getSummaries(),
                request.getKeywords()
        );
        return ApiResponse.success("뉴스피드가 성공적으로 수정되었습니다.", NewsFeedDetailResponse.from(updated));
    }

    @Operation(summary = "뉴스피드 삭제", description = "ID로 뉴스피드를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNewsFeed(@PathVariable("id") Long id) {
        newsFeedService.deleteNewsFeed(id);
        return ApiResponse.success("뉴스피드가 성공적으로 삭제되었습니다.", null);
    }
}
