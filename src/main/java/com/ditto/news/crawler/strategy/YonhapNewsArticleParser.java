package com.ditto.news.crawler.strategy;

import org.springframework.stereotype.Component;

/**
 * 연합뉴스 / Yonhap News Agency 기사 상세 페이지 파서.
 */
@Component
public class YonhapNewsArticleParser extends AbstractNewsArticleParser {

    private static final String[] SUPPORTED_DOMAINS = {"yna.co.kr"};

    private static final String[] TITLE_SELECTORS = {
            "h1.tit", "h1.tit-article", "h1.title-article", "div.title-article h1"
    };

    private static final String[] BODY_SELECTORS = {
            "article.story-news", "div.story-news", "div.article", "div#articleBody"
    };

    private static final String[] DATE_SELECTORS = {
            "p.update-time", "span.tt", "div.info-box span.txt-time", "p.txt-time"
    };

    @Override
    protected String[] getSupportedDomains() {
        return SUPPORTED_DOMAINS;
    }

    @Override
    protected String getDefaultSource() {
        return "Yonhap News";
    }

    @Override
    protected String[] getTitleSelectors() {
        return TITLE_SELECTORS;
    }

    @Override
    protected String[] getBodyContainerSelectors() {
        return BODY_SELECTORS;
    }

    @Override
    protected String[] getDateSelectors() {
        return DATE_SELECTORS;
    }
}
