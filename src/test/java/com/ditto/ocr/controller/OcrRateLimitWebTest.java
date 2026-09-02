package com.ditto.ocr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ditto.global.exception.GlobalExceptionHandler;
import com.ditto.ocr.config.OcrProperties;
import com.ditto.ocr.dto.response.OcrRecognitionResponse;
import com.ditto.ocr.service.OcrNavigationService;
import com.ditto.ocr.support.OcrRateLimitInterceptor;
import com.ditto.ocr.support.OcrRateLimiter;

/**
 * 실제 웹 스택(MockMvc)으로 OCR 인식 엔드포인트가 정상 동작하는지 + 레이트 리밋이
 * 실제 HTTP 응답(200 → 429)으로 이어지는지 검증한다. CLOVA·DB 는 서비스 목으로 대체한다.
 */
class OcrRateLimitWebTest {

    private static final String URL = "/api/v1/ocr/locations/recognize";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OcrNavigationService service = mock(OcrNavigationService.class);
        when(service.recognizeLocation(any())).thenReturn(OcrRecognitionResponse.builder()
                .recognitionId("ocr_test")
                .recognizedBrandName("TAMBURINS")
                .requiresSelection(false)
                .candidates(Collections.emptyList())
                .build());

        OcrProperties properties = new OcrProperties();
        properties.getRateLimit().setLimit(3); // 테스트용 소형 상한
        properties.getRateLimit().setWindow(Duration.ofMinutes(1));
        OcrRateLimitInterceptor interceptor = new OcrRateLimitInterceptor(new OcrRateLimiter(properties));

        this.mockMvc = MockMvcBuilders.standaloneSetup(new OcrLocationController(service))
                .addMappedInterceptors(new String[] {URL}, interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "sign.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    @DisplayName("정상 인식 요청은 200 과 인식 결과를 돌려준다")
    void recognizeReturnsOk() throws Exception {
        mockMvc.perform(multipart(URL).file(image()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recognizedBrandName").value("TAMBURINS"));
    }

    @Test
    @DisplayName("상한 이내는 200, 초과 요청부터 429(E005)로 거절된다")
    void rateLimitKicksInAfterLimit() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(multipart(URL).file(image()))
                    .andExpect(status().isOk());
        }
        // 4번째 요청 → 429
        mockMvc.perform(multipart(URL).file(image()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("E005"));
    }
}
