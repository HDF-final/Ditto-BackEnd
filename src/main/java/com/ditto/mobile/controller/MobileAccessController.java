package com.ditto.mobile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.course.dto.response.CourseDetailResponse;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.mobile.dto.request.IssueAccessCodeRequest;
import com.ditto.mobile.dto.request.SetLocationRequest;
import com.ditto.mobile.dto.request.VerifyAccessCodeRequest;
import com.ditto.mobile.dto.response.IssueAccessCodeResponse;
import com.ditto.mobile.dto.response.SetLocationResponse;
import com.ditto.mobile.service.MobileAccessService;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Mobile Access", description = "모바일 접속 코드·위치 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile")
public class MobileAccessController {

    private final MobileAccessService mobileAccessService;

    @Operation(
            summary = "모바일 접속 코드 발급",
            description = "로그인한 고객이 본인 코스에 대한 모바일 접속 코드를 발급한다. "
                    + "발급된 코드는 유효기간을 가지며, 회원이 코스를 불러오는 데 쓰인다.")
    @PostMapping("/access-codes")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<IssueAccessCodeResponse> issueAccessCode(
            @Valid @RequestBody IssueAccessCodeRequest request) {
        return ApiResponse.success("성공",
                mobileAccessService.issueAccessCode(SecurityUtils.requireUserId(), request.getCourseId()));
    }

    @Operation(
            summary = "접속 코드 검증·코스 불러오기",
            description = "접속 코드를 검증해 코스 상세를 돌려준다. 유효하지 않거나 만료된 코드는 "
                    + "N002(400) 로 처리된다. 로그인이 필요하다.")
    @PostMapping("/access-codes/verify")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CourseDetailResponse> verifyAccessCode(
            @Valid @RequestBody VerifyAccessCodeRequest request) {
        return ApiResponse.success("성공", mobileAccessService.verifyAccessCode(request.getAccessCode()));
    }

    @Operation(
            summary = "현재 위치 확인·경로 시작점 조회",
            description = "현재 위치(장소 ID)의 길찾기 시작점 식별자를 돌려준다. 서버 상태를 두지 않는 "
                    + "무상태 조회다. 로그인이 필요하다.")
    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SetLocationResponse> resolveStartPoint(
            @Valid @RequestBody SetLocationRequest request) {
        return ApiResponse.success("성공", mobileAccessService.resolveStartPoint(request.getPlaceId()));
    }
}
