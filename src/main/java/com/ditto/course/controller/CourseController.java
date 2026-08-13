package com.ditto.course.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.course.dto.request.AddCoursePlaceRequest;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.AddCoursePlaceResponse;
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
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "내 코스 생성·저장",
            description = "로그인한 사용자의 코스를 저장합니다. 장소 없이 호출하면 수동 모드의 빈 코스로 시작하며, "
                    + "placeIds 는 DB place 테이블에 있는 장소만 담을 수 있습니다.")
    @PostMapping("/api/v1/courses")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateCourseResponse> create(@Valid @RequestBody(required = false) CreateCourseRequest request) {
        CreateCourseRequest body = request == null
                ? new CreateCourseRequest(null, null, null)
                : request;
        return ApiResponse.success("성공", courseService.create(SecurityUtils.requireUserId(), body));
    }

    @Operation(
            summary = "내 코스에 장소 추가",
            description = "로그인한 사용자의 내 코스 지정 순서에 장소를 추가합니다. "
                    + "장소는 DB place 테이블에 존재해야 하며, 같은 코스에 이미 담긴 장소는 다시 추가할 수 없습니다.")
    @PostMapping("/api/users/me/courses/{courseId}/places")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AddCoursePlaceResponse> addPlace(
            @PathVariable Long courseId,
            @Valid @RequestBody AddCoursePlaceRequest request) {
        return ApiResponse.success("성공", courseService.addPlace(SecurityUtils.requireUserId(), courseId, request));
    }
}
