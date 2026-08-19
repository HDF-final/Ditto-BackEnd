package com.ditto.aicourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.fasterxml.jackson.databind.ObjectMapper;

class AiCourseRecommendationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiEngineClient aiEngineClient;
    private AiCourseRecommendationService service;

    @BeforeEach
    void setUp() {
        aiEngineClient = mock(AiEngineClient.class);
        service = new AiCourseRecommendationService(aiEngineClient);
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

    private RecommendedPlaceResponse firstPlace() {
        CourseChatResponse response = service.chat(1L,
                CourseChatRequest.builder().message("카리나 좋아하는 브랜드 보고 싶어").build());
        assertThat(response.getPlaces()).hasSize(1);
        return response.getPlaces().get(0);
    }

    private void stub(String engineJson) throws IOException {
        when(aiEngineClient.chat(any(), any()))
                .thenReturn(MAPPER.readValue(engineJson, AiEngineChatResponse.class));
    }
}
