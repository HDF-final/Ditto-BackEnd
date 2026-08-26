package com.ditto.community.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.community.dto.request.CreateCommentRequest;
import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.request.UpdateCommentRequest;
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.BookmarkResponse;
import com.ditto.community.dto.response.CommentResponse;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.LikeResponse;
import com.ditto.community.dto.response.PostImageUploadResponse;
import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.community.service.PostBookmarkService;
import com.ditto.community.service.PostCommentService;
import com.ditto.community.service.PostImageService;
import com.ditto.community.service.PostLikeService;
import com.ditto.community.service.PostService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.i18n.AcceptLanguageResolver;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post", description = "커뮤니티 게시글, 댓글, 좋아요 및 북마크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/courses")
public class PostController {

    private final PostService postService;
    private final PostImageService postImageService;
    private final PostCommentService postCommentService;
    private final PostLikeService postLikeService;
    private final PostBookmarkService postBookmarkService;

    @Operation(
            summary = "공개 코스 목록 조회",
            description = "커뮤니티에 공개된 코스 게시글 목록을 최신순으로 페이징 조회합니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<PublicCourseResponse>> getPublicCourses(
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(1~100, 기본 10)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ApiResponse.success("성공", postService.getPublicCourses(
                page, size, AcceptLanguageResolver.resolve(acceptLanguage)));
    }

    @Operation(
            summary = "공개 코스 상세 조회",
            description = "커뮤니티에 공개된 코스 게시글의 본문과 연결된 코스, 장소 목록 및 댓글 목록을 상세 조회합니다.")
    @GetMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PublicCourseDetailResponse> getPublicCourse(
            @Parameter(description = "조회할 게시글 ID", example = "1")
            @PathVariable Long postId,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ApiResponse.success("성공", postService.getPublicCourse(
                postId, AcceptLanguageResolver.resolve(acceptLanguage)));
    }

    @Operation(summary = "코스 게시글 작성", description = "로그인한 사용자가 본인 소유 코스를 커뮤니티 게시글로 공개합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateCoursePostResponse> createCoursePost(
            @Valid @RequestBody CreateCoursePostRequest request) {
        return ApiResponse.success("성공",
                postService.createCoursePost(SecurityUtils.requireUserId(), request));
    }

    @Operation(summary = "코스 게시글 수정", description = "로그인한 사용자가 자신이 작성한 코스 게시글의 제목과 내용을 수정합니다.")
    @PatchMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UpdateCoursePostResponse> updateCoursePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdateCoursePostRequest request) {
        return ApiResponse.success("성공",
                postService.updateCoursePost(SecurityUtils.requireUserId(), postId, request));
    }

    @Operation(summary = "코스 게시글 삭제", description = "로그인한 사용자가 자신이 작성한 코스 게시글을 소프트 삭제합니다.")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteCoursePost(@PathVariable Long postId) {
        postService.deleteCoursePost(SecurityUtils.requireUserId(), postId);
        return ApiResponse.success("성공", null);
    }

    @Operation(
            summary = "코스 게시글 사진 업로드",
            description = "작성자가 자신의 코스 게시글에 사진을 첨부합니다. 여러 장을 한 번에 올릴 수 있으며, "
                    + "응답으로 게시글에 첨부된 전체 사진 조회 URL 목록을 정렬 순으로 돌려줍니다.")
    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PostImageUploadResponse> uploadImages(
            @Parameter(description = "사진을 첨부할 게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "첨부할 사진 목록 (multipart/form-data)")
            @RequestPart("images") List<MultipartFile> images) {
        return ApiResponse.success("성공",
                postImageService.uploadImages(SecurityUtils.requireUserId(), postId, images));
    }

    @Operation(summary = "코스 게시글 댓글 작성", description = "로그인한 고객(ROLE_CUSTOMER)이 코스 게시글에 댓글을 작성합니다.")
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @Parameter(description = "댓글을 작성할 게시글 ID", example = "1")
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success("성공",
                postCommentService.createComment(SecurityUtils.requireUserId(), postId, request));
    }

    @Operation(summary = "코스 게시글 댓글 목록 조회", description = "코스 게시글에 등록된 댓글 목록을 순서대로 조회합니다.")
    @GetMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<CommentResponse>> getComments(
            @Parameter(description = "댓글을 조회할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공", postCommentService.getComments(postId));
    }

    @Operation(summary = "코스 게시글 댓글 수정", description = "로그인한 고객(ROLE_CUSTOMER)이 본인이 작성한 댓글을 수정합니다.")
    @PatchMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "수정할 댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.success("성공",
                postCommentService.updateComment(SecurityUtils.requireUserId(), postId, commentId, request));
    }

    @Operation(summary = "코스 게시글 댓글 삭제", description = "로그인한 고객(ROLE_CUSTOMER)이 본인이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "삭제할 댓글 ID", example = "1")
            @PathVariable Long commentId) {
        postCommentService.deleteComment(SecurityUtils.requireUserId(), postId, commentId);
        return ApiResponse.success("성공", null);
    }

    @Operation(summary = "공개 코스 좋아요 등록", description = "로그인한 고객(ROLE_CUSTOMER)이 공개 코스 게시글에 좋아요를 등록합니다.")
    @PostMapping("/{postId}/likes")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LikeResponse> addLike(
            @Parameter(description = "좋아요를 등록할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공",
                postLikeService.addLike(SecurityUtils.requireUserId(), postId));
    }

    @Operation(summary = "공개 코스 좋아요 취소", description = "로그인한 고객(ROLE_CUSTOMER)이 공개 코스 게시글의 좋아요를 취소합니다.")
    @DeleteMapping("/{postId}/likes")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LikeResponse> removeLike(
            @Parameter(description = "좋아요를 취소할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공",
                postLikeService.removeLike(SecurityUtils.requireUserId(), postId));
    }

    @Operation(summary = "공개 코스 북마크 등록", description = "로그인한 고객(ROLE_CUSTOMER)이 공개 코스 게시글을 북마크(저장)에 등록합니다.")
    @PostMapping("/{postId}/bookmarks")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<BookmarkResponse> addBookmark(
            @Parameter(description = "북마크를 등록할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공",
                postBookmarkService.addBookmark(SecurityUtils.requireUserId(), postId));
    }

    @Operation(summary = "공개 코스 북마크 취소", description = "로그인한 고객(ROLE_CUSTOMER)이 공개 코스 게시글의 북마크(저장)를 취소합니다.")
    @DeleteMapping("/{postId}/bookmarks")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<BookmarkResponse> removeBookmark(
            @Parameter(description = "북마크를 취소할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공",
                postBookmarkService.removeBookmark(SecurityUtils.requireUserId(), postId));
    }
}
