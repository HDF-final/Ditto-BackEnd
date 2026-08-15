package com.ditto.news.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부 뉴스 기사 URL로부터 HTML Document를 획득하는 공통 크롤링 컴포넌트.
 *
 * <p>타임아웃, User-Agent, 리다이렉트, 바디 크기 제한 등의 공통 HTTP 정책을 처리하며,
 * 비정상 URL, HTTP 오류 상태(4xx, 5xx), 타임아웃, 빈 응답 등을 검증하고
 * 일관된 {@link BusinessException}으로 예외를 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonNewsCrawler {

    private final CrawlerProperties properties;

    /**
     * 기본 설정(CrawlerProperties)을 사용하여 주어진 URL의 HTML Document를 가져옵니다.
     *
     * @param url 크롤링 대상 웹 페이지 URL
     * @return 파싱된 Jsoup Document
     * @throws BusinessException URL이 잘못되었거나 네트워크/HTTP 오류 발생 시
     */
    public Document fetchDocument(String url) {
        return fetchDocument(url, properties.getTimeoutMillis(), properties.getUserAgent());
    }

    /**
     * 지정된 타임아웃 및 User-Agent를 사용하여 주어진 URL의 HTML Document를 가져옵니다.
     *
     * @param url 크롤링 대상 웹 페이지 URL
     * @param timeoutMillis 연결 및 읽기 타임아웃(ms)
     * @param userAgent 요청에 사용할 User-Agent 헤더
     * @return 파싱된 Jsoup Document
     * @throws BusinessException URL이 잘못되었거나 네트워크/HTTP 오류 발생 시
     */
    public Document fetchDocument(String url, int timeoutMillis, String userAgent) {
        validateUrl(url);

        try {
            log.debug("뉴스 크롤링 요청 시작: url={}, timeout={}ms", url, timeoutMillis);

            Document document = Jsoup.connect(url)
                    .userAgent(userAgent != null ? userAgent : properties.getUserAgent())
                    .timeout(timeoutMillis > 0 ? timeoutMillis : properties.getTimeoutMillis())
                    .maxBodySize(properties.getMaxBodySizeBytes())
                    .followRedirects(properties.isFollowRedirects())
                    .ignoreHttpErrors(properties.isIgnoreHttpErrors())
                    .get();

            validateDocument(document, url);

            log.debug("뉴스 크롤링 요청 성공: url={}, title={}", url, document.title());
            return document;

        } catch (HttpStatusException e) {
            log.error("뉴스 크롤링 HTTP 오류: url={}, statusCode={}, message={}",
                    url, e.getStatusCode(), e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);

        } catch (SocketTimeoutException e) {
            log.error("뉴스 크롤링 타임아웃 발생: url={}, message={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);

        } catch (IOException e) {
            log.error("뉴스 크롤링 네트워크/입출력 오류: url={}, message={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);

        } catch (Exception e) {
            log.error("뉴스 크롤링 예기치 않은 오류: url={}, cause={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }
    }

    /**
     * HTML 문자열을 Jsoup Document로 파싱합니다.
     *
     * @param html HTML 원문 문자열
     * @param baseUri 상대 경로 해석을 위한 기준 URI
     * @return 파싱된 Jsoup Document
     * @throws BusinessException HTML이 비어있는 경우
     */
    public Document parseHtml(String html, String baseUri) {
        if (html == null || html.trim().isEmpty()) {
            log.error("HTML 파싱 실패: 빈 HTML 본문");
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return Jsoup.parse(html, baseUri != null ? baseUri : "");
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.error("URL 검증 실패: URL이 비어있음");
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            log.error("URL 검증 실패: 지원하지 않는 프로토콜 (url={})", url);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            new URL(trimmed);
        } catch (MalformedURLException e) {
            log.error("URL 검증 실패: 올바르지 않은 URL 형식 (url={})", url);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateDocument(Document document, String url) {
        if (document == null || document.body() == null || document.body().text().trim().isEmpty()) {
            log.error("크롤링 결과 검증 실패: 문서 본문이 비어있음 (url={})", url);
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }
    }
}
