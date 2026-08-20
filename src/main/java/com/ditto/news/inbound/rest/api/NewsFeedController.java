package com.ditto.news.inbound.rest.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.i18n.AcceptLanguageResolver;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.response.NewsFeedDetailResponse;
import com.ditto.news.inbound.rest.dto.response.NewsFeedSitemapResponse;
import com.ditto.news.inbound.rest.dto.response.NewsFeedSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 사용자용 공개 뉴스피드 조회 REST API 컨트롤러 (Inbound Web Adapter).
 * 목록 조회, 사이트맵 조회, newsId 상세 조회, 검색 유입용 slug 조회를 전담합니다.
 */
@Tag(name = "News", description = "K-컬처 트렌드 뉴스피드 조회 API")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsFeedController {

    private final NewsFeedService newsFeedService;

    @Operation(summary = "뉴스피드 목록 조회", description = "최신순으로 뉴스피드 목록을 페이징 조회합니다.")
    @GetMapping
    public ApiResponse<List<NewsFeedSummaryResponse>> getNewsFeedList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        ContentLanguage language = AcceptLanguageResolver.resolve(acceptLanguage);
        List<NewsFeed> feeds = newsFeedService.getNewsFeeds(page, size, language);

        List<NewsFeedSummaryResponse> response = feeds.stream()
                .map(NewsFeedSummaryResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "사이트맵용 뉴스 목록 조회", description = "검색엔진 사이트맵(/sitemap.xml) 생성에 필요한 뉴스피드의 slug, 생성일 등 최소 정보를 조회합니다.")
    @GetMapping("/sitemap")
    public ApiResponse<List<NewsFeedSitemapResponse>> getNewsFeedsForSitemap() {
        List<NewsFeed> feeds = newsFeedService.getNewsFeedsForSitemap();

        List<NewsFeedSitemapResponse> response = feeds.stream()
                .map(NewsFeedSitemapResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "뉴스피드 상세 조회", description = "newsId로 뉴스피드 상세 본문을 조회합니다.")
    @GetMapping("/{newsId}")
    public ApiResponse<NewsFeedDetailResponse> getNewsFeedById(
            @PathVariable("newsId") Long newsId,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        NewsFeed feed = newsFeedService.getNewsFeedById(
                newsId, AcceptLanguageResolver.resolve(acceptLanguage));
        return ApiResponse.success(NewsFeedDetailResponse.from(feed));
    }

    @Operation(summary = "검색 유입 콘텐츠 조회", description = "SEO 및 검색 유입용 고유 URL 슬러그로 뉴스피드 상세 본문을 조회합니다.")
    @GetMapping("/slug/{slug}")
    public ApiResponse<NewsFeedDetailResponse> getNewsFeedBySlug(
            @PathVariable("slug") String slug,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        NewsFeed feed = newsFeedService.getNewsFeedBySlug(
                slug, AcceptLanguageResolver.resolve(acceptLanguage));
        return ApiResponse.success(NewsFeedDetailResponse.from(feed));
    }
}
