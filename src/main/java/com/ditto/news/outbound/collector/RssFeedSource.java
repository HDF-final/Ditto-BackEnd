package com.ditto.news.outbound.collector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수집 대상 RSS 피드 정보 모델.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssFeedSource {

    /** 피드 출처 언론사명 (예: "The Korea Herald") */
    private String name;

    /** RSS/Atom 피드 URL */
    private String url;

    /** 기본 카테고리 태그 */
    private String defaultCategory;
}
