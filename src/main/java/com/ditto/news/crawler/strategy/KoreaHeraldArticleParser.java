package com.ditto.news.crawler.strategy;

import org.springframework.stereotype.Component;

/**
 * The Korea Herald 기사 상세 페이지 파서.
 */
@Component
public class KoreaHeraldArticleParser extends AbstractNewsArticleParser {

    private static final String[] SUPPORTED_DOMAINS = {"koreaherald.com"};

    private static final String[] TITLE_SELECTORS = {
            "h1.view_tit", "h1.article_title", "div.view_tit", "h1.headline"
    };

    private static final String[] BODY_SELECTORS = {
            "#articleText", "div#articleText", "div.view_con_t", "div.article_content", "div#article-body"
    };

    private static final String[] DATE_SELECTORS = {
            ".view_tit_by span", "div.view_tit_by", ".date_time", "p.posted_time"
    };

    @Override
    protected String[] getSupportedDomains() {
        return SUPPORTED_DOMAINS;
    }

    @Override
    protected String getDefaultSource() {
        return "The Korea Herald";
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
