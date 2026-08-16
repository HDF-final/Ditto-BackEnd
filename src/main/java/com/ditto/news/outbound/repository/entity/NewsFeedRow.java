package com.ditto.news.outbound.repository.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * news_feed 테이블 1:1 매핑 MyBatis Row 객체.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsFeedRow {

    private Long newsFeedId;
    private String title;
    private String slug;
    private String representativeImageUrl;
    private String body;
    private String summary;
    private String keywords;
    private LocalDateTime createdAt;
}
