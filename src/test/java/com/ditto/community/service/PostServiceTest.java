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

import java.util.List;

import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.course.dto.response.CoursePlaceResponse;
import com.ditto.course.repository.PostMapper.PublicCourseDetailPostRow;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.ContentTranslationService;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private com.ditto.community.repository.PostCommentMapper postCommentMapper;

    @Mock
    private ContentTranslationService contentTranslationService;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("본인 소유 코스로 게시글을 작성한다")
    void createCoursePost() {
        Course course = Course.of(100L, USER_ID, null, "나의 코스", null, CourseCreationType.MANUAL.name());
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
        Course course = Course.of(101L, USER_ID, 3L, "복사 코스", null, CourseCreationType.COPIED.name());
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
        Course course = Course.of(100L, 99L, null, "다른 사람 코스", null, CourseCreationType.MANUAL.name());
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
        Course course = Course.of(3L, null, null, "기본 코스", null, CourseCreationType.SYSTEM.name());
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
        Course course = Course.of(100L, USER_ID, null, "나의 코스", null, CourseCreationType.MANUAL.name());
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

    @Test
    @DisplayName("공개 코스 게시글 목록을 정상 조회하고 필드를 매핑한다")
    void getPublicCoursesSuccess() {
        PublicCourseResponse item = PublicCourseResponse.builder()
                .postId(1L)
                .courseId(3L)
                .title("내가 다녀온 K-MZ 코스")
                .writerNickname("Yuki_T")
                .likeCount(12L)
                .bookmarkCount(4L)
                .build();
        given(postMapper.findPublicCourses(0L, 10)).willReturn(List.of(item));
        given(postMapper.countPublicCourses()).willReturn(1L);

        PageResponse<PublicCourseResponse> response = postService.getPublicCourses(0, 10);

        assertThat(response.getPage()).isZero();
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getContent()).hasSize(1);

        PublicCourseResponse contentItem = response.getContent().get(0);
        assertThat(contentItem.getPostId()).isEqualTo(1L);
        assertThat(contentItem.getCourseId()).isEqualTo(3L);
        assertThat(contentItem.getTitle()).isEqualTo("내가 다녀온 K-MZ 코스");
        assertThat(contentItem.getWriterNickname()).isEqualTo("Yuki_T");
        assertThat(contentItem.getLikeCount()).isEqualTo(12L);
        assertThat(contentItem.getBookmarkCount()).isEqualTo(4L);

        verify(postMapper).findPublicCourses(0L, 10);
        verify(postMapper).countPublicCourses();
    }

    @Test
    @DisplayName("영어 요청이면 공개 코스 제목을 번역한다")
    void translatesPublicCourseTitle() {
        PublicCourseResponse item = PublicCourseResponse.builder()
                .postId(1L)
                .title("에스파 브랜드 투어")
                .build();
        given(postMapper.findPublicCourses(0L, 10)).willReturn(List.of(item));
        given(postMapper.countPublicCourses()).willReturn(1L);
        given(contentTranslationService.translate(
                "community_post", "1", "title", "에스파 브랜드 투어", ContentLanguage.ENGLISH))
                .willReturn("aespa brand tour");

        PageResponse<PublicCourseResponse> response = postService.getPublicCourses(
                0, 10, ContentLanguage.ENGLISH);

        assertThat(response.getContent().get(0).getTitle()).isEqualTo("aespa brand tour");
    }

    @Test
    @DisplayName("목록이 없을 때 빈 리스트와 totalElements 0을 반환한다")
    void getPublicCoursesEmpty() {
        given(postMapper.findPublicCourses(0L, 10)).willReturn(List.of());
        given(postMapper.countPublicCourses()).willReturn(0L);

        PageResponse<PublicCourseResponse> response = postService.getPublicCourses(0, 10);

        assertThat(response.getPage()).isZero();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("page와 size에 따른 offset 계산을 검증한다 (page=2, size=10 -> offset=20)")
    void getPublicCoursesOffsetCalculation() {
        given(postMapper.findPublicCourses(20L, 10)).willReturn(List.of());
        given(postMapper.countPublicCourses()).willReturn(50L);

        PageResponse<PublicCourseResponse> response = postService.getPublicCourses(2, 10);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(50L);
        verify(postMapper).findPublicCourses(20L, 10);
    }

    @Test
    @DisplayName("페이지 범위를 초과하면 빈 목록과 요청 page, 전체 개수를 반환한다")
    void getPublicCoursesBeyondRange() {
        given(postMapper.findPublicCourses(100L, 10)).willReturn(List.of());
        given(postMapper.countPublicCourses()).willReturn(5L);

        PageResponse<PublicCourseResponse> response = postService.getPublicCourses(10, 10);

        assertThat(response.getPage()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(5L);
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("음수 page 요청 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void rejectWhenNegativePage() {
        assertThatThrownBy(() -> postService.getPublicCourses(-1, 10))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postMapper, never()).findPublicCourses(any(Long.class), any(Integer.class));
        verify(postMapper, never()).countPublicCourses();
    }

    @Test
    @DisplayName("size가 0 이하 요청 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void rejectWhenSizeZeroOrNegative() {
        assertThatThrownBy(() -> postService.getPublicCourses(0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> postService.getPublicCourses(0, -5))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postMapper, never()).findPublicCourses(any(Long.class), any(Integer.class));
    }

    @Test
    @DisplayName("size가 최대 크기(100) 초과 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void rejectWhenSizeExceedsMax() {
        assertThatThrownBy(() -> postService.getPublicCourses(0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postMapper, never()).findPublicCourses(any(Long.class), any(Integer.class));
    }

    @Test
    @DisplayName("공개 코스 게시글 상세와 장소 목록을 정상 조회한다")
    void getPublicCourseSuccess() {
        PublicCourseDetailPostRow postRow = new PublicCourseDetailPostRow(1L, 3L, "내가 다녀온 K-MZ 코스", "추천 동선입니다.");
        CoursePlaceResponse place1 = new CoursePlaceResponse(11L, "더현대 서울", null, "1F", 1, null, null, null);
        CoursePlaceResponse place2 = new CoursePlaceResponse(22L, "IFC 몰", null, "B1", 2, null, null, null);

        given(postMapper.findPublicCourseDetailById(1L)).willReturn(Optional.of(postRow));
        given(courseMapper.findPlacesByCourseId(3L)).willReturn(List.of(place1, place2));
        given(postCommentMapper.findCommentsByPostId(1L)).willReturn(List.of());

        PublicCourseDetailResponse response = postService.getPublicCourse(1L);

        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("내가 다녀온 K-MZ 코스");
        assertThat(response.getContent()).isEqualTo("추천 동선입니다.");
        assertThat(response.getCourse()).isNotNull();
        assertThat(response.getCourse().getCourseId()).isEqualTo(3L);
        assertThat(response.getCourse().getPlaces()).hasSize(2);
        assertThat(response.getCourse().getPlaces().get(0).getPlaceId()).isEqualTo(11L);
        assertThat(response.getCourse().getPlaces().get(0).getOrder()).isEqualTo(1);
        assertThat(response.getCourse().getPlaces().get(1).getPlaceId()).isEqualTo(22L);
        assertThat(response.getCourse().getPlaces().get(1).getOrder()).isEqualTo(2);
        assertThat(response.getComments()).isEmpty();

        verify(postMapper).findPublicCourseDetailById(1L);
        verify(courseMapper).findPlacesByCourseId(3L);
        verify(postCommentMapper).findCommentsByPostId(1L);
    }

    @Test
    @DisplayName("영어 요청이면 게시글 제목과 본문만 번역한다")
    void translatesPublicCourseDetail() {
        PublicCourseDetailPostRow postRow = new PublicCourseDetailPostRow(
                1L, 3L, "에스파 브랜드 투어", "추천 동선입니다.");
        given(postMapper.findPublicCourseDetailById(1L)).willReturn(Optional.of(postRow));
        given(courseMapper.findPlacesByCourseId(3L)).willReturn(List.of());
        given(postCommentMapper.findCommentsByPostId(1L)).willReturn(List.of());
        given(contentTranslationService.translate(
                "community_post", "1", "title", "에스파 브랜드 투어", ContentLanguage.ENGLISH))
                .willReturn("aespa brand tour");
        given(contentTranslationService.translate(
                "community_post", "1", "content", "추천 동선입니다.", ContentLanguage.ENGLISH))
                .willReturn("This is the recommended route.");

        PublicCourseDetailResponse response = postService.getPublicCourse(
                1L, ContentLanguage.ENGLISH);

        assertThat(response.getTitle()).isEqualTo("aespa brand tour");
        assertThat(response.getContent()).isEqualTo("This is the recommended route.");
    }

    @Test
    @DisplayName("코스에 장소가 없으면 places 빈 배열을 반환한다")
    void getPublicCourseWithoutPlaces() {
        PublicCourseDetailPostRow postRow = new PublicCourseDetailPostRow(1L, 3L, "내가 다녀온 K-MZ 코스", "추천 동선입니다.");

        given(postMapper.findPublicCourseDetailById(1L)).willReturn(Optional.of(postRow));
        given(courseMapper.findPlacesByCourseId(3L)).willReturn(List.of());
        given(postCommentMapper.findCommentsByPostId(1L)).willReturn(List.of());

        PublicCourseDetailResponse response = postService.getPublicCourse(1L);

        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getCourse().getPlaces()).isEmpty();
        assertThat(response.getComments()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 게시글/코스인 경우 POST_NOT_FOUND 예외가 발생한다")
    void getPublicCourseNotFound() {
        given(postMapper.findPublicCourseDetailById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublicCourse(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(courseMapper, never()).findPlacesByCourseId(any());
    }

    @Test
    @DisplayName("postId가 null이거나 0 이하이면 POST_NOT_FOUND 예외가 발생한다")
    void getPublicCourseInvalidPostId() {
        assertThatThrownBy(() -> postService.getPublicCourse(null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        assertThatThrownBy(() -> postService.getPublicCourse(0L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        assertThatThrownBy(() -> postService.getPublicCourse(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
