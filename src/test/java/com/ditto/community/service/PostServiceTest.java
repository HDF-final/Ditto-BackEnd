package com.ditto.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostInsertCommand;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("본인 소유 코스로 게시글을 작성한다")
    void createCoursePost() {
        Course course = Course.of(100L, USER_ID, null, "나의 코스", null, CourseCreationType.MANUAL.name(), "ABCD1234");
        given(courseMapper.findById(100L)).willReturn(Optional.of(course));
        given(postMapper.insert(any(PostInsertCommand.class))).willAnswer(invocation -> {
            PostInsertCommand command = invocation.getArgument(0);
            command.setPostId(2L);
            return 1;
        });

        CreateCoursePostResponse response = postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(100L)
                        .title(" 나의 더현대 코스 ")
                        .content(" K-뷰티 중심 코스입니다. ")
                        .build());

        ArgumentCaptor<PostInsertCommand> captor = ArgumentCaptor.forClass(PostInsertCommand.class);
        verify(postMapper).insert(captor.capture());
        PostInsertCommand saved = captor.getValue();
        assertThat(saved.getCourseId()).isEqualTo(100L);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getTitle()).isEqualTo("나의 더현대 코스");
        assertThat(saved.getContent()).isEqualTo("K-뷰티 중심 코스입니다.");
        assertThat(response.getPostId()).isEqualTo(2L);
        assertThat(response.getCourseId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("나의 더현대 코스");
    }

    @Test
    @DisplayName("복사 후 본인 소유가 된 코스도 게시글을 작성할 수 있다")
    void createCoursePostWithCopiedOwnCourse() {
        Course course = Course.of(101L, USER_ID, 3L, "복사 코스", null, CourseCreationType.COPIED.name(), "COPY1234");
        given(courseMapper.findById(101L)).willReturn(Optional.of(course));
        given(postMapper.insert(any(PostInsertCommand.class))).willAnswer(invocation -> {
            PostInsertCommand command = invocation.getArgument(0);
            command.setPostId(3L);
            return 1;
        });

        CreateCoursePostResponse response = postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(101L)
                        .title("복사 코스 후기")
                        .content("복사한 코스를 공개합니다.")
                        .build());

        assertThat(response.getPostId()).isEqualTo(3L);
        assertThat(response.getCourseId()).isEqualTo(101L);
        verify(postMapper).insert(any(PostInsertCommand.class));
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 코스는 COURSE_NOT_FOUND")
    void rejectWhenCourseNotFound() {
        given(courseMapper.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(404L)
                        .title("제목")
                        .content("내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(postMapper, never()).insert(any());
    }

    @Test
    @DisplayName("다른 사용자 소유 코스는 게시글 작성이 거부된다")
    void rejectWhenCourseOwnedByOtherUser() {
        Course course = Course.of(100L, 99L, null, "다른 사람 코스", null, CourseCreationType.MANUAL.name(), "ABCD1234");
        given(courseMapper.findById(100L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(100L)
                        .title("제목")
                        .content("내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_OWNER);

        verify(postMapper, never()).insert(any());
    }

    @Test
    @DisplayName("SYSTEM 기본 코스는 직접 게시글 작성이 거부된다")
    void rejectWhenSystemCourse() {
        Course course = Course.of(3L, null, null, "기본 코스", null, CourseCreationType.SYSTEM.name(), "SYSTEM01");
        given(courseMapper.findById(3L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(3L)
                        .title("제목")
                        .content("내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_OWNER);

        verify(postMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Mapper INSERT 실패 시 트랜잭션 메서드에서 예외가 전파된다")
    void insertFailurePropagatesInTransaction() throws NoSuchMethodException {
        Course course = Course.of(100L, USER_ID, null, "나의 코스", null, CourseCreationType.MANUAL.name(), "ABCD1234");
        given(courseMapper.findById(100L)).willReturn(Optional.of(course));
        given(postMapper.insert(any(PostInsertCommand.class))).willThrow(new IllegalStateException("insert failed"));

        assertThatThrownBy(() -> postService.createCoursePost(
                USER_ID,
                CreateCoursePostRequest.builder()
                        .courseId(100L)
                        .title("제목")
                        .content("내용")
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("insert failed");

        Transactional transactional = PostService.class
                .getMethod("createCoursePost", Long.class, CreateCoursePostRequest.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
    }
}
