package com.ditto.aicourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.aicourse.client.AiEngineChatResponse;
import com.ditto.aicourse.client.AiEngineClient;
import com.ditto.aicourse.dto.request.CourseChatRequest;
import com.ditto.aicourse.dto.response.CourseChatResponse;
import com.ditto.aicourse.dto.response.RecommendedPlaceResponse;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
import com.ditto.global.i18n.ContentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiCourseRecommendationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiEngineClient aiEngineClient;
    private ContentTranslationService contentTranslationService;
    private AiCourseRecommendationService service;

    @BeforeEach
    void setUp() {
        aiEngineClient = mock(AiEngineClient.class);
        contentTranslationService = mock(ContentTranslationService.class);
        service = new AiCourseRecommendationService(aiEngineClient, contentTranslationService);
    }

    @Test
    @DisplayName("엔진이 준 사진을 버리지 않고 그대로 내보낸다")
    void keepsEngineImage() throws IOException {
        stub("""
                {"session":"ZDfsXQvMQns","reply":"준비했습니다","turn":1,
                 "places":[{"place_name":"프라다","navigation_key":"1F_STORE_0035","reason":"앰배서더",
                            "image":{"kind":"evidence",
                                     "url":"https://cdn.straightnews.co.kr/253829.jpg",
                                     "source":"cdn.straightnews.co.kr",
                                     "caption":"카리나 × Prada"}}]}
                """);

        RecommendedPlaceResponse place = firstPlace();

        assertThat(place.getPlaceName()).isEqualTo("프라다");
        assertThat(place.getImageUrl()).isEqualTo("https://cdn.straightnews.co.kr/253829.jpg");
        // kind 가 없으면 화면이 보도사진을 매장 외관으로 걸어 버린다.
        assertThat(place.getImage().getKind()).isEqualTo("evidence");
        assertThat(place.getImage().getSource()).isEqualTo("cdn.straightnews.co.kr");
        assertThat(place.getImage().getCaption()).isEqualTo("카리나 × Prada");
    }

    @Test
    @DisplayName("엔진이 평평한 image_url 을 보내면 그쪽을 쓴다")
    void prefersFlatImageUrl() throws IOException {
        stub("""
                {"session":"s","reply":"r","turn":1,
                 "places":[{"place_name":"프라다","navigation_key":"1F_STORE_0035","reason":"근거",
                            "image_url":"https://flat.example/a.jpg",
                            "image":{"kind":"place","url":"https://nested.example/b.jpg"}}]}
                """);

        assertThat(firstPlace().getImageUrl()).isEqualTo("https://flat.example/a.jpg");
    }

    @Test
    @DisplayName("사진을 못 구한 장소도 코스에서 빠지지 않는다")
    void survivesMissingImage() throws IOException {
        stub("""
                {"session":"s","reply":"r","turn":1,
                 "places":[{"place_name":"CH 1985","navigation_key":"6F_STORE_0025","reason":"문화"}]}
                """);

        RecommendedPlaceResponse place = firstPlace();

        assertThat(place.getNavigationKey()).isEqualTo("6F_STORE_0025");
        assertThat(place.getImageUrl()).isNull();
        assertThat(place.getImage()).isNull();
    }

    @Test
    @DisplayName("ditto-chat-v2 가 실제로 돌려준 모양을 그대로 옮긴다")
    void mapsLiveV2Payload() throws IOException {
        // ditto-chat-v2 를 실제로 호출해 받은 응답에서 장소 한 곳을 그대로 가져왔다.
        // url·image_url·image.url 이 셋 다 같은 값으로 오고, image 에 article·width·height 가 붙는다.
        stub("""
                {"session":"Kx7mQ2vLpNa","reply":"준비했습니다","turn":1,"llm_calls":7,"seconds":40.4,
                 "warnings":[],"_engine":"tavily","_input_tokens":75806,"_output_tokens":2810,
                 "places":[{"place_name":"프라다","navigation_key":"1F_STORE_0035","reason":"앰배서더",
                            "url":"https://img1.kakaocdn.net/a.webp",
                            "image_url":"https://img1.kakaocdn.net/a.webp",
                            "image":{"kind":"evidence","url":"https://img1.kakaocdn.net/a.webp",
                                     "source":"brunch.co.kr","caption":"카리나 × 프라다",
                                     "article":"https://brunch.co.kr/@jennafashion/8",
                                     "width":1080,"height":1350}}]}
                """);

        RecommendedPlaceResponse place = firstPlace();

        assertThat(place.getImageUrl()).isEqualTo("https://img1.kakaocdn.net/a.webp");
        assertThat(place.getImage().getArticle()).isEqualTo("https://brunch.co.kr/@jennafashion/8");
        assertThat(place.getImage().getWidth()).isEqualTo(1080);
        assertThat(place.getImage().getHeight()).isEqualTo(1350);
        // 엔진이 새로 붙인 _engine·_input_tokens 같은 키는 무시하고 넘어가야 한다.
        assertThat(place.getPlaceName()).isEqualTo("프라다");
    }

    @Test
    @DisplayName("평평한 필드가 없고 image 만 와도 사진을 찾아낸다")
    void fallsBackToNestedUrl() throws IOException {
        stub("""
                {"session":"s","reply":"r","turn":1,
                 "places":[{"place_name":"로바","navigation_key":"6F_STORE_0027","reason":"식사",
                            "image":{"kind":"place","url":"https://nested.example/b.jpg"}}]}
                """);

        assertThat(firstPlace().getImageUrl()).isEqualTo("https://nested.example/b.jpg");
    }

    @Test
    @DisplayName("응답시간과 토큰을 metrics 로 실어 보낸다")
    void reportsMetrics() throws IOException {
        stub("""
                {"session":"T8J2Q4kzq_o","reply":"준비했습니다","turn":2,"llm_calls":5,
                 "seconds":15.0,"_resumed":true,"_input_tokens":31797,"_output_tokens":765,
                 "places":[]}
                """);

        CourseChatResponse response = service.chat(1L,
                CourseChatRequest.builder().sessionId("T8J2Q4kzq_o").message("밥집만 바꿔줘").build());

        assertThat(response.getTurn()).isEqualTo(2);
        assertThat(response.getMetrics().getSeconds()).isEqualTo(15.0);
        assertThat(response.getMetrics().getLlmCalls()).isEqualTo(5);
        assertThat(response.getMetrics().getInputTokens()).isEqualTo(31797);
        assertThat(response.getMetrics().getOutputTokens()).isEqualTo(765);
    }

    @Test
    @DisplayName("엔진이 수치를 안 줘도 metrics 자리는 비워 두고 넘어간다")
    void toleratesMissingMetrics() throws IOException {
        stub("{\"session\":\"s\",\"reply\":\"r\",\"turn\":1,\"places\":[]}");

        CourseChatResponse response = service.chat(1L,
                CourseChatRequest.builder().message("코스 짜줘").build());

        assertThat(response.getMetrics()).isNotNull();
        assertThat(response.getMetrics().getSeconds()).isNull();
        assertThat(response.getMetrics().getInputTokens()).isNull();
    }

    @Test
    @DisplayName("장소 4분류를 그대로 내보낸다 — 매장·음식점·카페·여가")
    void keepsPlaceCategory() throws IOException {
        // ditto-chat-v2 를 실제로 호출해 받은 한 턴에서 네 종류가 다 나온 것을 옮겼다.
        stub("""
                {"session":"s","reply":"r","turn":1,"places":[
                  {"place_name":"프라다","navigation_key":"1F_STORE_0035","category":"매장","reason":"a"},
                  {"place_name":"나의 가야","navigation_key":"6F_STORE_0035","category":"음식점","reason":"b"},
                  {"place_name":"스타벅스 리저브","navigation_key":"B2_STORE_0028","category":"카페","reason":"c"},
                  {"place_name":"에픽서울","navigation_key":"5F_STORE_0047","category":"여가","reason":"d"}]}
                """);

        CourseChatResponse response = service.chat(1L,
                CourseChatRequest.builder().message("밥이랑 커피랑 전시").build());

        assertThat(response.getPlaces())
                .extracting(RecommendedPlaceResponse::getCategory)
                .containsExactly("매장", "음식점", "카페", "여가");
    }

    @Test
    @DisplayName("선택 언어를 엔진에 전달하고 설명성 응답만 번역한다")
    void localizesAiCourseDescriptions() throws IOException {
        stub("""
                {"session":"s","reply":"코스를 준비했어요","turn":1,"places":[
                  {"place_name":"프라다","navigation_key":"1F_STORE_0035","category":"매장",
                   "reason":"카리나가 즐겨 찾는 브랜드예요",
                   "image":{"kind":"evidence","url":"https://example.com/a.jpg",
                            "caption":"카리나가 프라다를 착용한 모습"}}]}
                """);
        when(contentTranslationService.translate(
                eq("ai_course_recommendation"), eq("s:1"), eq("reply"),
                eq("코스를 준비했어요"), eq(ContentLanguage.ENGLISH)))
                .thenReturn("Your course is ready.");
        when(contentTranslationService.translate(
                eq("ai_course_recommendation"), eq("s:1"), eq("place_0_reason"),
                eq("카리나가 즐겨 찾는 브랜드예요"), eq(ContentLanguage.ENGLISH)))
                .thenReturn("A brand often worn by Karina.");
        when(contentTranslationService.translate(
                eq("ai_course_recommendation"), eq("s:1"), eq("place_0_image_caption"),
                eq("카리나가 프라다를 착용한 모습"), eq(ContentLanguage.ENGLISH)))
                .thenReturn("Karina wearing Prada.");

        CourseChatResponse response = service.chat(
                1L,
                CourseChatRequest.builder().message("Make me a course").build(),
                ContentLanguage.ENGLISH);

        assertThat(response.getReply()).isEqualTo("Your course is ready.");
        assertThat(response.getPlaces().get(0).getPlaceName()).isEqualTo("프라다");
        assertThat(response.getPlaces().get(0).getCategory()).isEqualTo("매장");
        assertThat(response.getPlaces().get(0).getReason()).isEqualTo("A brand often worn by Karina.");
        assertThat(response.getPlaces().get(0).getImage().getCaption())
                .isEqualTo("Karina wearing Prada.");
        verify(aiEngineClient).chat(null, "Make me a course", ContentLanguage.ENGLISH);
    }

    @Test
    @DisplayName("엔진이 category 를 안 주던 시절 응답도 그대로 흘려보낸다")
    void toleratesMissingCategory() throws IOException {
        stub("{\"session\":\"s\",\"reply\":\"r\",\"turn\":1,"
                + "\"places\":[{\"place_name\":\"프라다\",\"navigation_key\":\"1F_STORE_0035\"}]}");

        assertThat(firstPlace().getCategory()).isNull();
    }

    private RecommendedPlaceResponse firstPlace() {
        CourseChatResponse response = service.chat(1L,
                CourseChatRequest.builder().message("카리나 좋아하는 브랜드 보고 싶어").build());
        assertThat(response.getPlaces()).hasSize(1);
        return response.getPlaces().get(0);
    }

    private void stub(String engineJson) throws IOException {
        when(aiEngineClient.chat(any(), any(), any()))
                .thenReturn(MAPPER.readValue(engineJson, AiEngineChatResponse.class));
    }
}
