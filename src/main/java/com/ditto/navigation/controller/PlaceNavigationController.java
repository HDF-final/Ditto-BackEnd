package com.ditto.navigation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.i18n.AcceptLanguageResolver;
import com.ditto.navigation.dto.response.MapAssetResponse;
import com.ditto.navigation.dto.response.PlaceNavigationResponse;
import com.ditto.navigation.service.MapAssetService;
import com.ditto.navigation.service.PlaceNavigationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Place Navigation", description = "길찾기 장소 식별자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceNavigationController {

    private final PlaceNavigationService placeNavigationService;
    private final MapAssetService mapAssetService;

    @Operation(
            summary = "길찾기 가능 장소 목록 조회",
            description = "navigation_key 가 등록된 장소의 placeId·navigationKey·이름·층을 조회합니다.")
    @GetMapping("/navigation")
    public ApiResponse<List<PlaceNavigationResponse>> getNavigablePlaces(
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ApiResponse.success("성공", placeNavigationService.getNavigablePlaces(
                AcceptLanguageResolver.resolve(acceptLanguage)));
    }

    /**
     * 원장 파일이 아니라 <b>주소</b>를 돌려준다. 588KB 를 백엔드로 통과시키면 CDN 을 둔
     * 이유가 사라진다 — 브라우저가 CloudFront 에서 직접 받는다.
     *
     * <p>{@code /{placeId}/navigation} 보다 먼저 두는 것은 읽는 사람을 위한 것이다.
     * 경로 매칭은 고정 문자열({@code navigation/assets})을 변수보다 구체적으로 본다.
     */
    @Operation(
            summary = "실내 지도 원장 자산 위치 조회",
            description = "층 그래프·장소 원장·방 폴리곤이 놓인 CDN 주소를 돌려줍니다. 파일 자체는 CDN 에서 직접 받습니다.")
    @GetMapping("/navigation/assets")
    public ApiResponse<MapAssetResponse> getMapAssets() {
        return ApiResponse.success("성공", mapAssetService.getAssets());
    }

    @Operation(summary = "장소 길찾기 식별자 조회")
    @GetMapping("/{placeId}/navigation")
    public ApiResponse<PlaceNavigationResponse> getNavigationByPlaceId(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ApiResponse.success("성공", placeNavigationService.getNavigationByPlaceId(
                placeId, AcceptLanguageResolver.resolve(acceptLanguage)));
    }
}
