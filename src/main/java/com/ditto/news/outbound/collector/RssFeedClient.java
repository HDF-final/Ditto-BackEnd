package com.ditto.news.outbound.collector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

/**
 * RSS/Atom 피드 원격 HTTP 요청 클라이언트.
 */
@Slf4j
@Component
public class RssFeedClient {

    private final RestClient restClient;

    public RssFeedClient(@Qualifier("rssFeedRestClient") RestClient rssFeedRestClient) {
        this.restClient = rssFeedRestClient;
    }

    /**
     * 지정된 피드 URL로 HTTP GET 요청을 보내고 XML 본문을 문자열로 수신합니다.
     *
     * @param feedUrl RSS/Atom 피드 URL
     * @return 피드 XML 본문 문자열
     */
    public String fetchXml(String feedUrl) {
        try {
            return restClient.get()
                    .uri(feedUrl)
                    .header("User-Agent", "Mozilla/5.0 (compatible; DittoNewsBot/1.0; +https://ditto.korea)")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("RSS 피드 HTTP 수신 실패: url={}, cause={}", feedUrl, e.getMessage());
            throw e;
        }
    }
}
