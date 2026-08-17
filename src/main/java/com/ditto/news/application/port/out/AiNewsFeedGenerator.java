package com.ditto.news.application.port.out;

import java.util.List;

import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;

/**
 * 선별된 기사 콘텐츠를 바탕으로 AI(LLM)를 활용하여 K-컬처 뉴스피드 콘텐츠를 생성하는 아웃바운드 포트.
 */
public interface AiNewsFeedGenerator {

    /**
     * 선별된 기사 목록과 토픽 키워드를 기반으로 AI 재작성 제목, 본문, 슬러그, 대표 이미지, 태그가 포함된 뉴스피드를 생성합니다.
     *
     * @param articles 선별된 기사 목록
     * @param topicKeyword 뉴스피드 주제 키워드
     * @return 생성된 뉴스피드 콘텐츠
     */
    GeneratedNewsFeed generate(List<CrawledNewsArticle> articles, String topicKeyword);
}
