package com.ditto.ocr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.ocr.dto.request.OcrSessionStartRequest;
import com.ditto.ocr.dto.response.OcrRecognitionResponse;
import com.ditto.ocr.dto.response.OcrSessionStartResponse;
import com.ditto.ocr.service.OcrNavigationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "OCR Navigation", description = "OCR 간판 인식 길찾기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ocr")
public class OcrNavigationController {

    private final OcrNavigationService ocrNavigationService;

    @Operation(
            summary = "OCR 길찾기 세션 시작",
            description = "현재 장소(placeId)와 진입 경로(source)로 세션을 발급한다. "
                    + "응답의 sessionId 를 이후 인식 요청에 실어 보낸다.")
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<OcrSessionStartResponse> startSession(
            @Valid @RequestBody OcrSessionStartRequest request) {
        return ApiResponse.success("성공", ocrNavigationService.startSession(request));
    }

    @Operation(
            summary = "OCR 간판 인식",
            description = "간판 이미지를 인식해 브랜드명과 매칭되는 장소 후보를 돌려준다. "
                    + "이미지 전처리 → CLOVA → 층·가격·% 후처리 → 카탈로그 매칭 순이다. "
                    + "프로모 문구는 단어 리스트가 아니라 매장 목록과 안 맞으면 버린다. "
                    + "후보의 confidence 는 OCR 신뢰도, matchScore 는 카탈로그 매칭 점수다. "
                    + "CLOVA 장애·타임아웃은 E002(502) 로 변환된다.")
    @PostMapping(value = "/recognitions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<OcrRecognitionResponse> recognize(
            @Parameter(description = "간판 이미지 (multipart/form-data)")
            @RequestPart("image") MultipartFile image,
            @Parameter(description = "세션 시작에서 발급받은 세션 ID", example = "session_01")
            @RequestParam("sessionId") String sessionId) {
        return ApiResponse.success("성공", ocrNavigationService.recognize(sessionId, image));
    }
}
