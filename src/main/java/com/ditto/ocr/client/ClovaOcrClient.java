package com.ditto.ocr.client;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.client.dto.ClovaOcrRequest;
import com.ditto.ocr.client.dto.ClovaOcrResponse;
import com.ditto.ocr.config.OcrProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 네이버 CLOVA OCR(General) 호출 클라이언트.
 *
 * <p>이미지를 멀티파트로 보내 인식 텍스트를 받는다. 브랜드↔장소 매칭은 하지 않는다 —
 * CLOVA 는 원본 텍스트와 신뢰도만 주고, 후보 장소를 고르는 일은 서비스가 맡는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaOcrClient {

    private static final String VERSION = "V2";

    private final RestClient clovaOcrRestClient;
    private final OcrProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 이미지 한 장을 인식한다.
     *
     * @param image    이미지 바이트
     * @param format   확장자(jpg·png 등). CLOVA 가 요구하는 포맷 힌트다.
     * @param fileName 원본 파일명(로그·멀티파트 파트명용)
     */
    public ClovaOcrResult recognize(byte[] image, String format, String fileName) {
        String messageJson = buildMessage(format);

        try {
            ClovaOcrResponse response = clovaOcrRestClient.post()
                    .uri(properties.getClova().getInvokeUrl())
                    .header("X-OCR-SECRET", properties.getClova().getSecret())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(buildMultipart(messageJson, image, fileName))
                    .retrieve()
                    .body(ClovaOcrResponse.class);

            return extractBrand(response);

        } catch (RestClientException e) {
            log.error("CLOVA OCR 호출 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.OCR_SERVICE_ERROR);
        }
    }

    private String buildMessage(String format) {
        ClovaOcrRequest request = ClovaOcrRequest.builder()
                .version(VERSION)
                .requestId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .images(List.of(ClovaOcrRequest.Image.builder()
                        .format(format)
                        .name("ocr")
                        .build()))
                .build();
        // message 는 JSON 문자열 파트로 보낸다. 멀티파트 파트에 그대로 싣기 위해
        // 객체가 아닌 문자열로 직렬화한다.
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("CLOVA OCR 요청 직렬화 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.OCR_SERVICE_ERROR);
        }
    }

    private org.springframework.util.MultiValueMap<String, org.springframework.http.HttpEntity<?>> buildMultipart(
            String messageJson, byte[] image, String fileName) {
        org.springframework.http.client.MultipartBodyBuilder builder =
                new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("message", messageJson, MediaType.APPLICATION_JSON);
        builder.part("file", new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return StringUtils.hasText(fileName) ? fileName : "image";
            }
        }).contentType(MediaType.APPLICATION_OCTET_STREAM);
        return builder.build();
    }

    /**
     * 응답 필드 중 바운딩 박스 면적이 가장 큰 글자를 브랜드명으로 채택한다.
     * 간판에서 가장 큰 글자가 상호일 가능성이 높다는 휴리스틱이다.
     */
    private ClovaOcrResult extractBrand(ClovaOcrResponse response) {
        if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
            return ClovaOcrResult.empty();
        }
        ClovaOcrResponse.Image image = response.getImages().get(0);
        if (image.getFields() == null || image.getFields().isEmpty()) {
            return ClovaOcrResult.empty();
        }

        return image.getFields().stream()
                .filter(f -> StringUtils.hasText(f.getInferText()))
                .max(Comparator.comparingDouble(this::area))
                .map(f -> new ClovaOcrResult(
                        f.getInferText().trim(),
                        f.getInferConfidence() == null ? 0.0 : f.getInferConfidence()))
                .orElse(ClovaOcrResult.empty());
    }

    /** 바운딩 박스 꼭짓점으로 사각형 면적을 근사한다(폭 × 높이). */
    private double area(ClovaOcrResponse.Field field) {
        if (field.getBoundingPoly() == null || field.getBoundingPoly().getVertices() == null) {
            return 0.0;
        }
        List<ClovaOcrResponse.Vertex> vertices = field.getBoundingPoly().getVertices();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (ClovaOcrResponse.Vertex v : vertices) {
            if (v.getX() == null || v.getY() == null) {
                continue;
            }
            minX = Math.min(minX, v.getX());
            minY = Math.min(minY, v.getY());
            maxX = Math.max(maxX, v.getX());
            maxY = Math.max(maxY, v.getY());
        }
        if (maxX < minX || maxY < minY) {
            return 0.0;
        }
        return (maxX - minX) * (maxY - minY);
    }
}
