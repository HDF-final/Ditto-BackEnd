package com.ditto.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.community.dto.request.CreateCommentRequest;
import com.ditto.community.dto.response.CommentResponse;
import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.service.PostCommentService;
import com.ditto.community.service.PostService;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.security.AuthUser;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private PostCommentService postCommentService;

    @MockBean
    private com.ditto.community.service.PostLikeService postLikeService;

    @MockBean
    private com.ditto.community.service.PostBookmarkService postBookmarkService;

    @Test
    @DisplayName("공개 코스 목록 조회 성공 시 ApiResponse와 PageResponse 형태로 반환한다")
    void getPublicCoursesSuccess() throws Exception {
        PublicCourseResponse item = PublicCourseResponse.builder()
                .postId(1L)
                .courseId(3L)
                .title("내가 다녀온 K-MZ 코스")
                .likeCount(12L)
                .bookmarkCount(4L)
                .build();
        PageResponse<PublicCourseResponse> pageResponse = new PageResponse<>(List.of(item), 0, 1L);
        given(postService.getPublicCourses(0, 10)).willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/community/courses")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].postId").value(1L))
                .andExpect(jsonPath("$.data.content[0].courseId").value(3L))
                .andExpect(jsonPath("$.data.content[0].title").value("내가 다녀온 K-MZ 코스"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(12L))
                .andExpect(jsonPath("$.data.content[0].bookmarkCount").value(4L));

        verify(postService).getPublicCourses(0, 10);
    }

    @Test
    @DisplayName("파라미터가 없으면 기본값 page=0, size=10을 적용한다")
    void getPublicCoursesDefaultParameters() throws Exception {
        PageResponse<PublicCourseResponse> emptyPage = new PageResponse<>(List.of(), 0, 0L);
        given(postService.getPublicCourses(0, 10)).willReturn(emptyPage);

        mockMvc.perform(get("/api/v1/community/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());

        verify(postService).getPublicCourses(0, 10);
    }

    @Test
    @DisplayName("잘못된 페이징 파라미터 요청 시 400 에러와 C001 코드를 반환한다")
    void getPublicCoursesInvalidParameter() throws Exception {
        given(postService.getPublicCourses(-1, 10))
                .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/v1/community/courses")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("공개 코스 상세 조회 성공 시 본문, 코스 장소 목록, 댓글 목록을 반환한다")
    void getPublicCourseSuccess() throws Exception {
        PublicCourseDetailResponse detail = PublicCourseDetailResponse.builder()
                .postId(1L)
                .title("내가 다녀온 K-MZ 코스")
                .content("추천 동선입니다.")
                .course(PublicCourseDetailResponse.CourseInfo.builder()
                        .courseId(3L)
                        .places(List.of(
                                PublicCourseDetailResponse.PlaceInfo.builder()
                                        .placeId(11L)
                                        .order(1)
                                        .build()))
                        .build())
                .comments(List.of(
                        CommentResponse.builder()
                                .commentId(101L)
                                .postId(1L)
                                .userId(2L)
                                .nickname("Chen_Li")
                                .isAuthor(false)
                                .content("오전에 가기 좋아요!")
                                .createdAt(LocalDateTime.now())
                                .build()))
                .build();
        given(postService.getPublicCourse(1L)).willReturn(detail);

        mockMvc.perform(get("/api/v1/community/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.data.postId").value(1L))
                .andExpect(jsonPath("$.data.title").value("내가 다녀온 K-MZ 코스"))
                .andExpect(jsonPath("$.data.content").value("추천 동선입니다."))
                .andExpect(jsonPath("$.data.course.courseId").value(3L))
                .andExpect(jsonPath("$.data.course.places[0].placeId").value(11L))
                .andExpect(jsonPath("$.data.course.places[0].order").value(1))
                .andExpect(jsonPath("$.data.comments[0].commentId").value(101L))
                .andExpect(jsonPath("$.data.comments[0].nickname").value("Chen_Li"));

        verify(postService).getPublicCourse(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 상세 조회 시 404 에러와 CM001 코드를 반환한다")
    void getPublicCourseNotFound() throws Exception {
        given(postService.getPublicCourse(999L))
                .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/community/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CM001"))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("댓글 작성 성공 시 201 Created와 생성된 댓글 정보를 반환한다")
    void createCommentSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?")
                .build();

        CommentResponse response = CommentResponse.builder()
                .commentId(1L)
                .postId(10L)
                .userId(2L)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?")
                .createdAt(LocalDateTime.of(2026, 8, 17, 14, 26, 0))
                .build();

        given(postCommentService.createComment(eq(2L), eq(10L), any(CreateCommentRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/community/courses/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.commentId").value(1L))
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.nickname").value("Chen_Li"))
                .andExpect(jsonPath("$.data.isAuthor").value(false))
                .andExpect(jsonPath("$.data.content").value("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?"));

        verify(postCommentService).createComment(eq(2L), eq(10L), any(CreateCommentRequest.class));
    }

    @Test
    @DisplayName("댓글 내용이 비어있으면 400 Bad Request 에러를 반환한다")
    void createCommentBlankContentValidationError() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("")
                .build();

        mockMvc.perform(post("/api/v1/community/courses/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 시 200 OK와 댓글 리스트를 반환한다")
    void getCommentsSuccess() throws Exception {
        CommentResponse c1 = CommentResponse.builder()
                .commentId(1L)
                .postId(10L)
                .userId(2L)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("워터폴 가든은 오전에 가면 사람이 적어서 사진 찍기 좋아요.")
                .createdAt(LocalDateTime.of(2026, 8, 17, 14, 14, 0))
                .build();

        given(postCommentService.getComments(10L)).willReturn(List.of(c1));

        mockMvc.perform(get("/api/v1/community/courses/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].commentId").value(1L))
                .andExpect(jsonPath("$.data[0].nickname").value("Chen_Li"))
                .andExpect(jsonPath("$.data[0].content").value("워터폴 가든은 오전에 가면 사람이 적어서 사진 찍기 좋아요."));

        verify(postCommentService).getComments(10L);
    }

    @Test
    @DisplayName("댓글 삭제 성공 시 200 OK와 성공 메시지를 반환한다")
    void deleteCommentSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/community/courses/10/comments/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("성공"));

        verify(postCommentService).deleteComment(2L, 10L, 101L);
    }

    @Test
    @DisplayName("댓글 수정 성공 시 200 OK와 수정된 댓글 정보를 반환한다")
    void updateCommentSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        com.ditto.community.dto.request.UpdateCommentRequest request =
                com.ditto.community.dto.request.UpdateCommentRequest.builder()
                        .content("수정된 댓글 내용입니다.")
                        .build();

        CommentResponse response = CommentResponse.builder()
                .commentId(101L)
                .postId(10L)
                .userId(2L)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("수정된 댓글 내용입니다.")
                .createdAt(LocalDateTime.of(2026, 8, 17, 14, 26, 0))
                .build();

        given(postCommentService.updateComment(eq(2L), eq(10L), eq(101L), any(com.ditto.community.dto.request.UpdateCommentRequest.class)))
                .willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/community/courses/10/comments/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.commentId").value(101L))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글 내용입니다."));

        verify(postCommentService).updateComment(eq(2L), eq(10L), eq(101L), any(com.ditto.community.dto.request.UpdateCommentRequest.class));
    }

    @Test
    @DisplayName("좋아요 등록 성공 시 200 OK와 업데이트된 좋아요 정보를 반환한다")
    void addLikeSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        com.ditto.community.dto.response.LikeResponse response = com.ditto.community.dto.response.LikeResponse.builder()
                .postId(10L)
                .likesCount(78)
                .isLiked(true)
                .build();

        given(postLikeService.addLike(2L, 10L)).willReturn(response);

        mockMvc.perform(post("/api/v1/community/courses/10/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.likesCount").value(78))
                .andExpect(jsonPath("$.data.isLiked").value(true));

        verify(postLikeService).addLike(2L, 10L);
    }

    @Test
    @DisplayName("좋아요 취소 성공 시 200 OK와 업데이트된 좋아요 정보를 반환한다")
    void removeLikeSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        com.ditto.community.dto.response.LikeResponse response = com.ditto.community.dto.response.LikeResponse.builder()
                .postId(10L)
                .likesCount(77)
                .isLiked(false)
                .build();

        given(postLikeService.removeLike(2L, 10L)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/community/courses/10/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.likesCount").value(77))
                .andExpect(jsonPath("$.data.isLiked").value(false));

        verify(postLikeService).removeLike(2L, 10L);
    }

    @Test
    @DisplayName("북마크 등록 성공 시 200 OK와 업데이트된 북마크 정보를 반환한다")
    void addBookmarkSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        com.ditto.community.dto.response.BookmarkResponse response = com.ditto.community.dto.response.BookmarkResponse.builder()
                .postId(10L)
                .bookmarkCount(35)
                .isBookmarked(true)
                .build();

        given(postBookmarkService.addBookmark(2L, 10L)).willReturn(response);

        mockMvc.perform(post("/api/v1/community/courses/10/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.bookmarkCount").value(35))
                .andExpect(jsonPath("$.data.isBookmarked").value(true));

        verify(postBookmarkService).addBookmark(2L, 10L);
    }

    @Test
    @DisplayName("북마크 취소 성공 시 200 OK와 업데이트된 북마크 정보를 반환한다")
    void removeBookmarkSuccess() throws Exception {
        AuthUser principal = new AuthUser(2L, "customer@test.com", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        com.ditto.community.dto.response.BookmarkResponse response = com.ditto.community.dto.response.BookmarkResponse.builder()
                .postId(10L)
                .bookmarkCount(34)
                .isBookmarked(false)
                .build();

        given(postBookmarkService.removeBookmark(2L, 10L)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/community/courses/10/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.postId").value(10L))
                .andExpect(jsonPath("$.data.bookmarkCount").value(34))
                .andExpect(jsonPath("$.data.isBookmarked").value(false));

        verify(postBookmarkService).removeBookmark(2L, 10L);
    }
}
