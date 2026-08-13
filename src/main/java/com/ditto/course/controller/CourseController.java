package com.ditto.course.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.course.dto.request.AddCoursePlaceRequest;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.AddCoursePlaceResponse;
import com.ditto.course.dto.request.UpdateCourseRequest;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.dto.response.UpdateCourseResponse;
import com.ditto.course.service.CourseService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "내 코스 목록 조회",
            description = "로그인한 사용자의 코스를 최신 생성순으로 페이징 조회합니다. "
                    + "응답 data 는 { content: [{ courseId, name, placeCount }], page, totalElements } 형태입니다.")
    @GetMapping("/my")
    public ApiResponse<PageResponse<MyCourseSummaryResponse>> getMyCourses(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(1~100, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("성공",
                courseService.getMyCourses(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(
            summary = "내 코스 삭제",
            description = "로그인한 사용자 본인의 코스를 삭제합니다(soft delete). 본인 코스가 아니면 거부됩니다.")
    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "삭제할 코스 ID", example = "100")
            @PathVariable Long courseId) {
        courseService.delete(SecurityUtils.requireUserId(), courseId);
        return ApiResponse.success();
    }

    @Operation(
            summary = "내 코스 정보·방문 순서 수정",
            description = "로그인한 사용자 본인 코스의 이름·설명을 수정하고, orderedPlaceIds 순서대로 방문 순서를 재정렬합니다. "
                    + "orderedPlaceIds 는 코스에 속한 장소 전체여야 합니다.")
    @PatchMapping("/{courseId}")
    public ApiResponse<UpdateCourseResponse> update(
            @Parameter(description = "수정할 코스 ID", example = "100")
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ApiResponse.success("성공",
                courseService.update(SecurityUtils.requireUserId(), courseId, request));
    }
  
    @Operation(
            summary = "내 코스에 장소 추가",
            description = "로그인한 사용자의 내 코스 지정 순서에 장소를 추가합니다. "
                    + "장소는 DB place 테이블에 존재해야 하며, 같은 코스에 이미 담긴 장소는 다시 추가할 수 없습니다.")
    @PostMapping("/{courseId}/places")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AddCoursePlaceResponse> addPlace(
            @PathVariable Long courseId,
            @Valid @RequestBody AddCoursePlaceRequest request) {
        return ApiResponse.success("성공", courseService.addPlace(SecurityUtils.requireUserId(), courseId, request));
    }

    @Operation(
            summary = "내 코스에서 장소 삭제",
            description = "로그인한 사용자의 내 코스에서 지정한 장소를 삭제하고 남은 장소의 방문 순서를 앞으로 당깁니다.")
    @DeleteMapping("/{courseId}/places/{placeId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deletePlace(
            @PathVariable Long courseId,
            @PathVariable Long placeId) {
        courseService.deletePlace(SecurityUtils.requireUserId(), courseId, placeId);
        return ApiResponse.success("성공", null);
    }
}
