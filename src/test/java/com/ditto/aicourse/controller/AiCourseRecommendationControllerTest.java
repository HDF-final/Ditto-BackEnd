package com.ditto.aicourse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.aicourse.dto.request.CourseChatRequest;
import com.ditto.aicourse.dto.response.CourseChatResponse;
import com.ditto.aicourse.dto.response.PlaceProductImageResponse;
import com.ditto.aicourse.dto.response.PlaceReservationResponse;
import com.ditto.aicourse.service.AiCourseRecommendationService;
import com.ditto.aicourse.service.PlaceProductImageService;
import com.ditto.aicourse.service.PlaceReservationService;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.security.AuthUser;

@WebMvcTest(AiCourseRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiCourseRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiCourseRecommendationService aiCourseRecommendationService;

    @MockBean
    private PlaceProductImageService placeProductImageService;

    @MockBean
    private PlaceReservationService placeReservationService;

    @Test
    @DisplayName("Accept-Language를 AI 추천 서비스 언어로 전달한다")
    void forwardsRequestedLanguage() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        given(aiCourseRecommendationService.chat(
                eq(2L), any(CourseChatRequest.class), eq(ContentLanguage.ENGLISH)))
                .willReturn(CourseChatResponse.builder()
                        .sessionId("session-1")
                        .reply("Your course is ready.")
                        .turn(1)
                        .places(List.of())
                        .build());

        mockMvc.perform(post("/api/v1/ai/course-recommendations/chat")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Make me a K-pop course\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("Your course is ready."));

        verify(aiCourseRecommendationService).chat(
                eq(2L), any(CourseChatRequest.class), eq(ContentLanguage.ENGLISH));
    }

    @Test
    @DisplayName("navigationKey로 장소 브랜드 상품 이미지를 조회한다")
    void getsPlaceProductImages() throws Exception {
        given(placeProductImageService.getProductImages("B2_STORE_0012", 3))
                .willReturn(List.of(PlaceProductImageResponse.builder()
                        .productId(10L)
                        .productName("뉴발란스 574")
                        .brandId(3L)
                        .brandName("뉴발란스")
                        .imageUrl("https://image.example.com/nb-574.jpg")
                        .productUrl("https://www.nbkorea.com/product/574")
                        .build()));

        mockMvc.perform(get("/api/v1/ai/course-recommendations/places/B2_STORE_0012/products")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productName").value("뉴발란스 574"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://image.example.com/nb-574.jpg"))
                .andExpect(jsonPath("$.data[0].productUrl").value("https://www.nbkorea.com/product/574"));

        verify(placeProductImageService).getProductImages("B2_STORE_0012", 3);
    }

    @Test
    @DisplayName("navigationKey로 장소의 캐치테이블 예약 링크를 조회한다")
    void getsPlaceReservation() throws Exception {
        given(placeReservationService.getReservation("B2_STORE_0049"))
                .willReturn(PlaceReservationResponse.builder()
                        .provider("CATCH_TABLE")
                        .placeName("ETF 베이커리")
                        .reservationUrl("https://www.catchtable.net/shop/etfbakerthehyundai")
                        .build());

        mockMvc.perform(get("/api/v1/ai/course-recommendations/places/B2_STORE_0049/reservation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("CATCH_TABLE"))
                .andExpect(jsonPath("$.data.placeName").value("ETF 베이커리"))
                .andExpect(jsonPath("$.data.reservationUrl")
                        .value("https://www.catchtable.net/shop/etfbakerthehyundai"));

        verify(placeReservationService).getReservation("B2_STORE_0049");
    }

    @Test
    @DisplayName("예약 링크가 없는 장소는 data가 null이다")
    void getsPlaceReservationNull() throws Exception {
        given(placeReservationService.getReservation("6F_STORE_0001")).willReturn(null);

        mockMvc.perform(get("/api/v1/ai/course-recommendations/places/6F_STORE_0001/reservation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(placeReservationService).getReservation("6F_STORE_0001");
    }
}
