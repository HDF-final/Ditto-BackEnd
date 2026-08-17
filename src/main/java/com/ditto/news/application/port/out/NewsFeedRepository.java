package com.ditto.news.application.port.out;

import java.util.List;
import java.util.Optional;

import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsFeed;

/**
 * 뉴스피드 영속화 및 데이터베이스 연동 아웃바운드 포트.
 */
public interface NewsFeedRepository {

    /**
     * AI가 생성한 뉴스피드를 DB에 INSERT 저장하고 영속화된 도메인 객체를 반환합니다.
     */
    NewsFeed save(GeneratedNewsFeed generatedFeed);

    /**
     * PK ID로 뉴스피드를 단건 조회합니다.
     */
    Optional<NewsFeed> findById(Long newsFeedId);

    /**
     * URL Slug로 뉴스피드를 단건 조회합니다.
     */
    Optional<NewsFeed> findBySlug(String slug);

    /**
     * 뉴스피드 목록을 페이지 단위로 조회합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 뉴스피드 도메인 엔티티 목록
     */
    List<NewsFeed> findAll(int page, int size);

    /**
     * 사이트맵 생성을 위한 전체 활성 뉴스피드의 경량 정보 목록을 조회합니다.
     */
    List<NewsFeed> findAllForSitemap();

    /**
     * 전체 뉴스피드 건수를 조회합니다.
     */
    long count();

    /**
     * 뉴스피드 도메인 객체의 변경사항을 DB에 UPDATE 반영합니다.
     */
    void update(NewsFeed newsFeed);

    /**
     * PK ID로 뉴스피드를 삭제합니다.
     */
    void deleteById(Long newsFeedId);
}
