package com.ditto.news.outbound.repository.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.news.outbound.repository.entity.NewsFeedRow;

/**
 * 뉴스피드 MyBatis Mapper 인터페이스.
 */
@Mapper
public interface NewsFeedMapper {

    /**
     * 뉴스피드를 news_feed 테이블에 INSERT 합니다.
     */
    int insert(
            @Param("title") String title,
            @Param("slug") String slug,
            @Param("representativeImageUrl") String representativeImageUrl,
            @Param("body") String body,
            @Param("summary") String summary,
            @Param("keywords") String keywords
    );

    /**
     * PK ID로 뉴스피드 단건을 조회합니다.
     */
    Optional<NewsFeedRow> findById(@Param("newsFeedId") Long newsFeedId);

    /**
     * URL slug로 뉴스피드 단건을 조회합니다.
     */
    Optional<NewsFeedRow> findBySlug(@Param("slug") String slug);

    /**
     * 뉴스피드 목록을 최신순으로 페이징 조회합니다.
     */
    List<NewsFeedRow> findAll(
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 전체 뉴스피드 건수를 조회합니다.
     */
    long count();

    /**
     * 뉴스피드 내용을 수정합니다.
     */
    int update(
            @Param("newsFeedId") Long newsFeedId,
            @Param("title") String title,
            @Param("body") String body,
            @Param("representativeImageUrl") String representativeImageUrl,
            @Param("summary") String summary,
            @Param("keywords") String keywords
    );

    /**
     * PK ID로 뉴스피드를 삭제합니다.
     */
    int deleteById(@Param("newsFeedId") Long newsFeedId);
}
