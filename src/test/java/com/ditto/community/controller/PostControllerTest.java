package com.ditto.community.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.service.PostService;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

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
    @DisplayName("공개 코스 상세 조회 성공 시 본문, 코스 장소 목록, 댓글 빈 배열을 반환한다")
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
                .comments(List.of())
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
                .andExpect(jsonPath("$.data.comments").isArray())
                .andExpect(jsonPath("$.data.comments").isEmpty());

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
}
