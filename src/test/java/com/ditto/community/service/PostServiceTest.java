package com.ditto.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostInsertCommand;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.course.repository.PostMapper.PostUpdateCommand;
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

    @Test
    @DisplayName("본인이 작성한 게시글의 제목과 내용을 수정한다")
    void updateCoursePost() {
        PostRow post = new PostRow(2L, 100L, USER_ID, "기존 제목", "기존 내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));
        given(postMapper.update(any(PostUpdateCommand.class))).willReturn(1);

        UpdateCoursePostResponse response = postService.updateCoursePost(
                USER_ID,
                2L,
                UpdateCoursePostRequest.builder()
                        .title(" 수정된 제목 ")
                        .content(" 수정된 내용 ")
                        .build());

        ArgumentCaptor<PostUpdateCommand> captor = ArgumentCaptor.forClass(PostUpdateCommand.class);
        verify(postMapper).update(captor.capture());
        PostUpdateCommand command = captor.getValue();
        assertThat(command.getPostId()).isEqualTo(2L);
        assertThat(command.getUserId()).isEqualTo(USER_ID);
        assertThat(command.getTitle()).isEqualTo("수정된 제목");
        assertThat(command.getContent()).isEqualTo("수정된 내용");
        assertThat(response.getPostId()).isEqualTo(2L);
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 게시글은 POST_NOT_FOUND")
    void rejectUpdateWhenPostNotFound() {
        given(postMapper.findActiveById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updateCoursePost(
                USER_ID,
                404L,
                UpdateCoursePostRequest.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postMapper, never()).update(any());
    }

    @Test
    @DisplayName("다른 사용자가 작성한 게시글은 수정할 수 없다")
    void rejectUpdateWhenPostWrittenByOtherUser() {
        PostRow post = new PostRow(2L, 100L, 99L, "기존 제목", "기존 내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updateCoursePost(
                USER_ID,
                2L,
                UpdateCoursePostRequest.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(postMapper, never()).update(any());
    }

    @Test
    @DisplayName("UPDATE 결과가 0건이면 POST_NOT_FOUND")
    void rejectUpdateWhenUpdatedCountZero() {
        PostRow post = new PostRow(2L, 100L, USER_ID, "기존 제목", "기존 내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));
        given(postMapper.update(any(PostUpdateCommand.class))).willReturn(0);

        assertThatThrownBy(() -> postService.updateCoursePost(
                USER_ID,
                2L,
                UpdateCoursePostRequest.builder()
                        .title("수정 제목")
                        .content("수정 내용")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 수정은 트랜잭션 메서드에서 실행된다")
    void updateCoursePostIsTransactional() throws NoSuchMethodException {
        Transactional transactional = PostService.class
                .getMethod("updateCoursePost", Long.class, Long.class, UpdateCoursePostRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    @Test
    @DisplayName("본인이 작성한 게시글을 소프트 삭제한다")
    void deleteCoursePost() {
        PostRow post = new PostRow(2L, 100L, USER_ID, "제목", "내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));
        given(postMapper.softDelete(2L, USER_ID)).willReturn(1);

        postService.deleteCoursePost(USER_ID, 2L);

        verify(postMapper).softDelete(2L, USER_ID);
        verify(courseMapper, never()).softDelete(any());
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 게시글은 삭제할 수 없다")
    void rejectDeleteWhenPostNotFound() {
        given(postMapper.findActiveById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deleteCoursePost(USER_ID, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postMapper, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("다른 사용자가 작성한 게시글은 삭제할 수 없다")
    void rejectDeleteWhenPostWrittenByOtherUser() {
        PostRow post = new PostRow(2L, 100L, 99L, "제목", "내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteCoursePost(USER_ID, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(postMapper, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("소프트 삭제 UPDATE 결과가 0건이면 POST_NOT_FOUND")
    void rejectDeleteWhenDeletedCountZero() {
        PostRow post = new PostRow(2L, 100L, USER_ID, "제목", "내용", 3, 4, null, null);
        given(postMapper.findActiveById(2L)).willReturn(Optional.of(post));
        given(postMapper.softDelete(2L, USER_ID)).willReturn(0);

        assertThatThrownBy(() -> postService.deleteCoursePost(USER_ID, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postMapper).softDelete(eq(2L), eq(USER_ID));
    }

    @Test
    @DisplayName("게시글 삭제는 트랜잭션 메서드에서 실행된다")
    void deleteCoursePostIsTransactional() throws NoSuchMethodException {
        Transactional transactional = PostService.class
                .getMethod("deleteCoursePost", Long.class, Long.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }
}
