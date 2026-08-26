package com.ditto.ocr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.ocr.dto.response.OcrRecognitionResponse;
import com.ditto.ocr.service.OcrNavigationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "OCR Location", description = "OCR 현재 위치 인식 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ocr/locations")
public class OcrLocationController {

    private final OcrNavigationService ocrNavigationService;

    @Operation(
            summary = "OCR 현재 위치 인식",
            description = "간판 이미지를 인식해 브랜드명과 매칭되는 장소 후보를 돌려준다. "
                    + "세션 없이 이미지만으로 동작한다. 이미지는 CLOVA 호출 전에 축소하고, "
                    + "층·가격·할인율만 형태로 버린 뒤 카탈로그 exact/alias/fuzzy 로 장소를 고른다. "
                    + "SALE·세일중 은 리스트로 지우지 않고 매장과 안 맞으면 후보에서 떨어진다. "
                    + "후보의 confidence 는 OCR 신뢰도, "
                    + "matchScore 는 카탈로그 매칭 점수다. CLOVA 장애·타임아웃은 E002(502) 로 변환된다.")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<OcrRecognitionResponse> recognize(
            @Parameter(description = "간판 이미지 (multipart/form-data)")
            @RequestPart("image") MultipartFile image) {
        return ApiResponse.success("성공", ocrNavigationService.recognizeLocation(image));
    }
}
