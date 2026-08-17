package com.ditto.news.outbound.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.outbound.generator.dto.GeminiNewsFeedPayload;

@ExtendWith(MockitoExtension.class)
class GeminiNewsFeedGeneratorTest {

    @Mock
    private GeminiNewsApiClient geminiClient;

    private GeminiNewsFeedGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new GeminiNewsFeedGenerator(geminiClient);
    }

    @Test
    @DisplayName("Gemini LLM 호출 성공 시 제목, 3줄 요약 리스트, 본문, 키워드가 포함된 뉴스피드를 생성한다")
    void generatesNewsFeedViaGeminiSuccessfully() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("New Jeans World Tour Announcement")
                .body("New Jeans announced their first world tour starting in Seoul.")
                .url("https://www.yna.co.kr/view/1")
                .source("Yonhap News")
                .imageUrl("https://img.yna.co.kr/photo1.jpg")
                .build();

        CrawledNewsArticle article2 = CrawledNewsArticle.builder()
                .title("BTS Member Solo Album Success")
                .body("BTS member reached top charts on Billboard.")
                .url("https://www.koreaherald.com/view/2")
                .source("The Korea Herald")
                .imageUrl(null)
                .build();

        GeminiNewsFeedPayload mockPayload = GeminiNewsFeedPayload.builder()
                .title("K-POP 서머 차트 돌풍… 뉴진스부터 BTS 솔로까지 컴백 열기")
                .summaries(List.of(
                        "뉴진스 서울을 시작으로 첫 월드투어 공식 발표",
                        "BTS 솔로 앨범 빌보드 메인 차트 상위권 진입",
                        "글로벌 팬덤 소비가 해외 음원 스트리밍으로 확대"
                ))
                .body("올여름 가요계가 대형 아티스트들의 연이은 신보 발매로 뜨겁게 달아오르고 있습니다.")
                .keywords(List.of("#KPOP", "#뉴진스", "#BTS", "#빌보드1위"))
                .build();

        given(geminiClient.generateRewrittenNews(anyList(), eq("K-POP")))
                .willReturn(mockPayload);

        GeneratedNewsFeed result = generator.generate(List.of(article1, article2), "K-POP");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("K-POP 서머 차트 돌풍… 뉴진스부터 BTS 솔로까지 컴백 열기");
        assertThat(result.getSummaries()).hasSize(3);
        assertThat(result.getSummaries().get(0)).isEqualTo("뉴진스 서울을 시작으로 첫 월드투어 공식 발표");
        assertThat(result.getBody()).contains("올여름 가요계가 대형 아티스트들의 연이은 신보 발매로", "출처: [Yonhap News](https://www.yna.co.kr/view/1)");
        assertThat(result.getRepresentativeImageUrl()).isEqualTo("https://img.yna.co.kr/photo1.jpg");
        assertThat(result.getKeywords()).contains("#뉴진스", "#BTS");
        assertThat(result.getSlug()).startsWith("k-pop-");
    }

    @Test
    @DisplayName("Gemini 미설정/실패 시 기사 본문에서 아티스트명(#뉴진스)과 이벤트명(#월드투어)을 동적으로 추출하고 본문 노이즈를 정제한다")
    void generatesNewsFeedViaTemplateFallbackWithDynamicArtistTags() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("New Jeans World Tour Announcement")
                .body("(서울=연합뉴스) 김기자 기자 = New Jeans announced their first world tour starting in Seoul. 2026.8.6 test@yna.co.kr")
                .url("https://www.yna.co.kr/view/1")
                .source("Yonhap News")
                .imageUrl("https://img.yna.co.kr/photo1.jpg")
                .build();

        given(geminiClient.generateRewrittenNews(anyList(), eq("K-POP")))
                .willReturn(null); // 폴백 상황

        GeneratedNewsFeed result = generator.generate(List.of(article1), "K-POP");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Jeans World Tour Announcement");
        assertThat(result.getSummaries()).isNotEmpty();
        assertThat(result.getBody()).contains("New Jeans announced their first world tour starting in Seoul.", "출처: [Yonhap News](https://www.yna.co.kr/view/1)");
        assertThat(result.getBody()).doesNotContain("test@yna.co.kr", "김기자 기자 =");
        assertThat(result.getRepresentativeImageUrl()).isEqualTo("https://img.yna.co.kr/photo1.jpg");
        assertThat(result.getKeywords()).contains("#뉴진스", "#월드투어");
        assertThat(result.getSlug()).startsWith("k-pop-");
    }

    @Test
    @DisplayName("기사 목록이 비어있거나 null인 경우 null을 반환한다")
    void returnsNullWhenArticlesEmpty() {
        assertThat(generator.generate(null, "K-POP")).isNull();
        assertThat(generator.generate(Collections.emptyList(), "K-POP")).isNull();
    }
}
