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
                .keywords(List.of("#KPOP", "#뉴진스", "#BTS"))
                .build();

        given(geminiClient.generateRewrittenNews(anyList(), eq("K-POP")))
                .willReturn(mockPayload);

        GeneratedNewsFeed result = generator.generate(List.of(article1, article2), "K-POP");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("K-POP 서머 차트 돌풍… 뉴진스부터 BTS 솔로까지 컴백 열기");
        assertThat(result.getSummaries()).hasSize(3);
        assertThat(result.getSummaries().get(0)).isEqualTo("뉴진스 서울을 시작으로 첫 월드투어 공식 발표");
        assertThat(result.getBody()).contains("올여름 가요계가 대형 아티스트들의 연이은 신보 발매로", "출처: Yonhap News, The Korea Herald");
        assertThat(result.getRepresentativeImageUrl()).isEqualTo("https://img.yna.co.kr/photo1.jpg");
        assertThat(result.getKeywords()).containsExactly("#KPOP", "#뉴진스", "#BTS");
        assertThat(result.getSlug()).startsWith("k-pop-");
    }

    @Test
    @DisplayName("Gemini 미설정/실패 시 안전하게 템플릿 기반 폴백 뉴스피드(요약 포함)를 생성한다")
    void generatesNewsFeedViaTemplateFallbackWhenGeminiFails() {
        CrawledNewsArticle article1 = CrawledNewsArticle.builder()
                .title("New Jeans World Tour Announcement")
                .body("New Jeans announced their first world tour starting in Seoul.")
                .url("https://www.yna.co.kr/view/1")
                .source("Yonhap News")
                .imageUrl("https://img.yna.co.kr/photo1.jpg")
                .build();

        given(geminiClient.generateRewrittenNews(anyList(), eq("K-POP")))
                .willReturn(null); // 폴백 상황

        GeneratedNewsFeed result = generator.generate(List.of(article1), "K-POP");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).contains("K-POP", "New Jeans World Tour Announcement");
        assertThat(result.getSummaries()).isNotEmpty();
        assertThat(result.getBody()).contains("New Jeans announced", "출처: Yonhap News");
        assertThat(result.getRepresentativeImageUrl()).isEqualTo("https://img.yna.co.kr/photo1.jpg");
        assertThat(result.getKeywords()).contains("#K-POP", "#KCulture", "#DITTO");
        assertThat(result.getSlug()).startsWith("k-pop-");
    }

    @Test
    @DisplayName("기사 목록이 비어있거나 null인 경우 null을 반환한다")
    void returnsNullWhenArticlesEmpty() {
        assertThat(generator.generate(null, "K-POP")).isNull();
        assertThat(generator.generate(Collections.emptyList(), "K-POP")).isNull();
    }
}
