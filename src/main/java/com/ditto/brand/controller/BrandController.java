package com.ditto.brand.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.brand.dto.response.BrandResponse;
import com.ditto.brand.service.BrandService;
import com.ditto.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Brand", description = "브랜드 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    @Operation(
            summary = "브랜드 목록 조회",
            description = "ACTIVE 브랜드의 brandId·이름·로고(presigned URL)를 조회합니다.")
    @GetMapping
    public ApiResponse<List<BrandResponse>> getBrands() {
        return ApiResponse.success("성공", brandService.getBrands());
    }

    @Operation(summary = "브랜드 단건 조회")
    @GetMapping("/{brandId}")
    public ApiResponse<BrandResponse> getBrand(
            @Parameter(description = "브랜드 ID", example = "1")
            @PathVariable Long brandId) {
        return ApiResponse.success("성공", brandService.getBrand(brandId));
    }
}
