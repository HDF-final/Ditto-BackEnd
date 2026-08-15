package com.ditto.news.crawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;

class CommonNewsCrawlerTest {

    private static HttpServer server;
    private static int serverPort;
    private CommonNewsCrawler crawler;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = server.getAddress().getPort();

        // 1. 정상 HTML 응답
        server.createContext("/news/sample", exchange -> {
            byte[] response = """
                    <!DOCTYPE html>
                    <html>
                    <head><title>K-컬처 팝업스토어 성황</title></head>
                    <body>
                      <h1>성수동 팝업스토어 현장</h1>
                      <p>K-컬처 트렌드가 전 세계적으로 주목받고 있습니다.</p>
                    </body>
                    </html>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // 2. 본문(body) 텍스트가 빈 HTML 응답
        server.createContext("/news/empty-body", exchange -> {
            byte[] response = "<html><head><title>Empty</title></head><body>   </body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // 3. HTTP 500 에러
        server.createContext("/news/error-500", exchange -> {
            byte[] response = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        CrawlerProperties properties = CrawlerProperties.builder()
                .timeoutMillis(2000)
                .userAgent("Ditto-Test-Crawler")
                .maxBodySizeBytes(1024 * 1024)
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .build();
        crawler = new CommonNewsCrawler(properties);
    }

    @Test
    @DisplayName("정상 HTTP 200 HTML 응답을 Jsoup Document로 파싱하여 제목과 본문을 확인한다")
    void fetchesAndParsesHtmlSuccessfully() {
        String url = "http://127.0.0.1:" + serverPort + "/news/sample";

        Document document = crawler.fetchDocument(url);

        assertThat(document).isNotNull();
        assertThat(document.title()).isEqualTo("K-컬처 팝업스토어 성황");
        assertThat(document.select("h1").text()).isEqualTo("성수동 팝업스토어 현장");
        assertThat(document.select("p").text()).contains("K-컬처 트렌드");
    }

    @Test
    @DisplayName("유효하지 않은 URL(null, 빈문자열, 비HTTP 프로토콜) 입력 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void validatesInvalidUrls() {
        assertThatThrownBy(() -> crawler.fetchDocument(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        assertThatThrownBy(() -> crawler.fetchDocument("   "))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        assertThatThrownBy(() -> crawler.fetchDocument("ftp://example.com/news"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        assertThatThrownBy(() -> crawler.fetchDocument("file:///path/to/file"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("HTTP 500 또는 존재하지 않는 서버 접속 실패 시 NEWS_CRAWLING_FAILED 예외가 발생한다")
    void throwsCrawlingFailedOnHttpOrNetworkError() {
        String errorUrl = "http://127.0.0.1:" + serverPort + "/news/error-500";
        assertThatThrownBy(() -> crawler.fetchDocument(errorUrl))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_CRAWLING_FAILED));

        String nonExistentUrl = "http://127.0.0.1:1/news/sample"; // 미사용 포트
        assertThatThrownBy(() -> crawler.fetchDocument(nonExistentUrl))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_CRAWLING_FAILED));
    }

    @Test
    @DisplayName("본문(body)이 비어있는 문서는 NEWS_CRAWLING_FAILED 예외를 던진다")
    void rejectsEmptyDocumentBody() {
        String emptyBodyUrl = "http://127.0.0.1:" + serverPort + "/news/empty-body";

        assertThatThrownBy(() -> crawler.fetchDocument(emptyBodyUrl))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_CRAWLING_FAILED));
    }

    @Test
    @DisplayName("parseHtml로 정상 HTML 문자열을 Document로 파싱하고 빈 본문은 INVALID_INPUT_VALUE 예외를 던진다")
    void parsesHtmlString() {
        String html = "<html><head><title>Direct Parse</title></head><body><p>Hello World</p></body></html>";
        Document doc = crawler.parseHtml(html, "https://ditto.test");
        assertThat(doc.title()).isEqualTo("Direct Parse");
        assertThat(doc.select("p").text()).isEqualTo("Hello World");

        assertThatThrownBy(() -> crawler.parseHtml(null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        assertThatThrownBy(() -> crawler.parseHtml("   ", null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}
