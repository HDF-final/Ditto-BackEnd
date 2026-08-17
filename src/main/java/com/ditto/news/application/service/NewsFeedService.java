package com.ditto.news.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.domain.NewsFeed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스피드 조회 및 도메인 상태 변경 비즈니스 서비스 (Application Core).
 * Inbound Web 어댑터 계층에 의존하지 않으며 순수 도메인 엔티티({@link NewsFeed})를 다룹니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsFeedService {

    private final NewsFeedRepository newsFeedRepository;

    /**
     * 뉴스피드 목록을 최신순으로 페이징 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 뉴스피드 도메인 엔티티 목록
     */
    public List<NewsFeed> getNewsFeeds(int page, int size) {
        return newsFeedRepository.findAll(page, size);
    }

    /**
     * 사이트맵(/sitemap.xml) 생성을 위해 전체 활성 뉴스피드의 경량 정보를 조회합니다.
     */
    public List<NewsFeed> getNewsFeedsForSitemap() {
        return newsFeedRepository.findAllForSitemap();
    }

    /**
     * PK ID로 뉴스피드 도메인 엔티티를 조회합니다.
     */
    public NewsFeed getNewsFeedById(Long newsFeedId) {
        return newsFeedRepository.findById(newsFeedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NEWS_FEED_NOT_FOUND));
    }

    /**
     * URL Slug로 뉴스피드 도메인 엔티티를 조회합니다.
     */
    public NewsFeed getNewsFeedBySlug(String slug) {
        return newsFeedRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.NEWS_FEED_NOT_FOUND));
    }

    /**
     * 뉴스피드 내용을 수정하고 수정된 도메인 엔티티를 반환합니다.
     */
    @Transactional
    public NewsFeed updateNewsFeed(
            Long newsFeedId,
            String title,
            String body,
            String representativeImageUrl,
            List<String> summaries,
            List<String> keywords) {

        NewsFeed existing = getNewsFeedById(newsFeedId);

        NewsFeed updated = NewsFeed.builder()
                .newsFeedId(existing.getNewsFeedId())
                .title(title)
                .slug(existing.getSlug())
                .representativeImageUrl(representativeImageUrl != null
                        ? representativeImageUrl
                        : existing.getRepresentativeImageUrl())
                .body(body)
                .summaries(summaries != null ? summaries : existing.getSummaries())
                .keywords(keywords != null ? keywords : existing.getKeywords())
                .createdAt(existing.getCreatedAt())
                .build();

        newsFeedRepository.update(updated);
        log.info("뉴스피드 수정 완료: newsFeedId={}", newsFeedId);

        return updated;
    }

    /**
     * PK ID로 뉴스피드를 삭제합니다.
     */
    @Transactional
    public void deleteNewsFeed(Long newsFeedId) {
        // 존재 여부 확인 (존재하지 않으면 NEWS_FEED_NOT_FOUND 예외)
        getNewsFeedById(newsFeedId);

        newsFeedRepository.deleteById(newsFeedId);
        log.info("뉴스피드 삭제 완료: newsFeedId={}", newsFeedId);
    }
}
