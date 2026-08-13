package com.ditto.course.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.service.CourseService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Course", description = "내 코스 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "내 코스 생성·저장",
            description = "로그인한 사용자의 코스를 저장합니다. 장소 없이 호출하면 수동 모드의 빈 코스로 시작하며, "
                    + "placeIds 는 DB place 테이블에 있는 장소만 담을 수 있습니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateCourseResponse> create(@Valid @RequestBody(required = false) CreateCourseRequest request) {
        CreateCourseRequest body = request == null
                ? new CreateCourseRequest(null, null, null)
                : request;
        return ApiResponse.success("성공", courseService.create(SecurityUtils.requireUserId(), body));
    }
}
