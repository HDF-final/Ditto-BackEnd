package com.ditto.navigation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.navigation.dto.response.PlaceNavigationResponse;
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

    @Operation(
            summary = "길찾기 가능 장소 목록 조회",
            description = "navigation_key 가 등록된 장소의 placeId·navigationKey·이름·층을 조회합니다.")
    @GetMapping("/navigation")
    public ApiResponse<List<PlaceNavigationResponse>> getNavigablePlaces() {
        return ApiResponse.success("성공", placeNavigationService.getNavigablePlaces());
    }

    @Operation(summary = "장소 길찾기 식별자 조회")
    @GetMapping("/{placeId}/navigation")
    public ApiResponse<PlaceNavigationResponse> getNavigationByPlaceId(
            @Parameter(description = "장소 ID", example = "1")
            @PathVariable Long placeId) {
        return ApiResponse.success("성공", placeNavigationService.getNavigationByPlaceId(placeId));
    }
}
