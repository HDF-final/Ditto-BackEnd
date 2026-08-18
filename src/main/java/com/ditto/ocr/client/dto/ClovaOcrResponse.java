package com.ditto.ocr.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CLOVA OCR General API 응답 본문.
 *
 * <pre>
 * {"images": [{"inferResult": "SUCCESS", "fields": [
 *     {"inferText": "TAMBURINS", "inferConfidence": 0.9994,
 *      "boundingPoly": {"vertices": [{"x": 10, "y": 20}, ...]}}]}]}
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClovaOcrResponse {

    private List<Image> images;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String inferResult;
        private String message;
        private List<Field> fields;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Field {
        private String inferText;
        private Double inferConfidence;
        private BoundingPoly boundingPoly;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoundingPoly {
        private List<Vertex> vertices;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vertex {
        private Double x;
        private Double y;
    }
}
