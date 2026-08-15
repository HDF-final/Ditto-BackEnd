package com.ditto.news.service.collector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 수집 대상 RSS 피드 정보.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RssFeedSource {

    /** 피드 식별자 / 언론사명 (예: "The Korea Herald", "Yonhap News") */
    private String name;

    /** RSS/Atom 피드 URL */
    private String url;

    /** 피드 기본 카테고리 (예: "Culture", "Entertainment") */
    private String defaultCategory;
}
