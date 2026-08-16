package com.ditto.news.outbound.crawler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Python 뉴스 크롤러 서비스 호출 요청 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCrawlRequest {

    /** 크롤링 대상 기사 URL */
    private String url;
}
