package com.ditto.news.inbound.rest.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.inbound.rest.dto.request.NewsFeedUpdateRequest;
import com.ditto.news.inbound.rest.dto.response.NewsFeedDetailResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 관리자(ROLE_ADMIN) 전용 뉴스피드 관리 REST API 컨트롤러 (Inbound Web Adapter).
 * 뉴스피드 수정(PATCH /api/v1/admin/news/{newsId}) 및 삭제(DELETE /api/v1/admin/news/{newsId})를 전담합니다.
 */
@Tag(name = "Admin - News", description = "관리자 전용 뉴스피드 관리 API")
@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
public class NewsFeedAdminController {

    private final NewsFeedService newsFeedService;

    @Operation(summary = "뉴스피드 수정 (관리자 전용)", description = "뉴스피드 제목, 본문, 대표이미지, 요약, 태그를 수정합니다. (ROLE_ADMIN 전용)")
    @PatchMapping("/{newsId}")
    public ApiResponse<NewsFeedDetailResponse> updateNewsFeed(
            @PathVariable("newsId") Long newsId,
            @Valid @RequestBody NewsFeedUpdateRequest request) {
        NewsFeed updated = newsFeedService.updateNewsFeed(
                newsId,
                request.getTitle(),
                request.getBody(),
                request.getRepresentativeImageUrl(),
                request.getSummaries(),
                request.getKeywords()
        );
        return ApiResponse.success("뉴스피드가 성공적으로 수정되었습니다.", NewsFeedDetailResponse.from(updated));
    }

    @Operation(summary = "뉴스피드 삭제 (관리자 전용)", description = "ID로 뉴스피드를 삭제합니다. (ROLE_ADMIN 전용)")
    @DeleteMapping("/{newsId}")
    public ApiResponse<Void> deleteNewsFeed(@PathVariable("newsId") Long newsId) {
        newsFeedService.deleteNewsFeed(newsId);
        return ApiResponse.success("뉴스피드가 성공적으로 삭제되었습니다.", null);
    }
}
