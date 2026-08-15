package com.ditto.news.crawler.strategy;

import org.springframework.stereotype.Component;

/**
 * The Korea Times 기사 상세 페이지 파서.
 */
@Component
public class KoreaTimesArticleParser extends AbstractNewsArticleParser {

    private static final String[] SUPPORTED_DOMAINS = {"koreatimes.co.kr"};

    private static final String[] TITLE_SELECTORS = {
            "div.view_head h1", "h1.headline", "div.view_tit", "h1.view_title"
    };

    private static final String[] BODY_SELECTORS = {
            "#article-body", "div#articleBody", "div.view_article", "div.article_body"
    };

    private static final String[] DATE_SELECTORS = {
            "div.view_head_info .date", "span.date", "div.date_area", "div.view_info span"
    };

    @Override
    protected String[] getSupportedDomains() {
        return SUPPORTED_DOMAINS;
    }

    @Override
    protected String getDefaultSource() {
        return "The Korea Times";
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
