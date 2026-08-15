package com.ditto.news.pipeline;

import java.util.List;

import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.GeneratedNewsFeed;

/**
 * 선별된 기사 콘텐츠를 바탕으로 LLM을 활용하여 K-컬처 뉴스피드 콘텐츠를 생성하는 역할 인터페이스.
 */
public interface NewsFeedGenerator {

    /**
     * 선별된 기사 목록과 토픽 키워드를 기반으로 제목, 본문, 슬러그, 대표 이미지, 태그가 포함된 뉴스피드를 생성합니다.
     *
     * @param articles 선별된 기사 목록
     * @param topicKeyword 뉴스피드 주제 키워드
     * @return LLM으로 생성된 뉴스피드 콘텐츠
     */
    GeneratedNewsFeed generate(List<CrawledNewsArticle> articles, String topicKeyword);
}
