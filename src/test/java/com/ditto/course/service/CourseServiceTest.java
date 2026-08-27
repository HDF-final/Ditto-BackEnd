package com.ditto.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.domain.VisitStatus;
import com.ditto.course.dto.request.AddCoursePlaceRequest;
import com.ditto.course.dto.response.AddCoursePlaceResponse;
import com.ditto.course.dto.response.CopyCourseResponse;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.request.UpdateCourseRequest;
import com.ditto.course.dto.response.CourseDetailResponse;
import com.ditto.course.dto.response.CoursePlaceResponse;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.dto.response.UpdateCourseResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CourseInsertCommand;
import com.ditto.course.repository.CourseMapper.CoursePlaceInsertCommand;
import com.ditto.course.repository.PlaceMapper;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private PlaceMapper placeMapper;

    @Mock
    private S3Provider s3Provider;

    @Mock
    private ContentTranslationService contentTranslationService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void addPlaceInsertsPlaceAtRequestedPosition() {
        Long userId = 7L;
        Long courseId = 10L;
        Long placeId = 20L;

        when(courseMapper.findById(courseId))
                .thenReturn(Optional.of(Course.of(
                        courseId,
                        userId,
                        null,
                        "나의 코스",
                        null,
                        CourseCreationType.MANUAL.name())));
        when(placeMapper.findExistingIds(List.of(placeId))).thenReturn(List.of(placeId));
        when(courseMapper.countPlaceInCourse(courseId, placeId)).thenReturn(0);
        when(courseMapper.findMaxVisitOrder(courseId)).thenReturn(2);

        AddCoursePlaceResponse response = courseService.addPlace(
                userId,
                courseId,
                AddCoursePlaceRequest.builder()
                        .placeId(placeId)
                        .position(2)
                        .build());

        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.getPlaceId()).isEqualTo(placeId);
        assertThat(response.getPosition()).isEqualTo(2);

        verify(courseMapper).markVisitOrdersForShift(courseId, 2);
        verify(courseMapper).incrementMarkedVisitOrders(courseId);

        ArgumentCaptor<CoursePlaceInsertCommand> captor = ArgumentCaptor.forClass(CoursePlaceInsertCommand.class);
        verify(courseMapper).insertPlace(captor.capture());

        CoursePlaceInsertCommand command = captor.getValue();
        assertThat(command.getCourseId()).isEqualTo(courseId);
        assertThat(command.getPlaceId()).isEqualTo(placeId);
        assertThat(command.getVisitOrder()).isEqualTo(2);
        assertThat(command.getRecommendationReason()).isNull();
        assertThat(command.getVisitStatus()).isEqualTo(VisitStatus.PENDING.name());
    }

    @Test
    void addPlaceRejectsDuplicatePlaceInCourse() {
        Long userId = 7L;
        Long courseId = 10L;
        Long placeId = 20L;

        when(courseMapper.findById(courseId))
                .thenReturn(Optional.of(Course.of(
                        courseId,
                        userId,
                        null,
                        "나의 코스",
                        null,
                        CourseCreationType.MANUAL.name())));
        when(placeMapper.findExistingIds(List.of(placeId))).thenReturn(List.of(placeId));
        when(courseMapper.countPlaceInCourse(courseId, placeId)).thenReturn(1);

        assertThatThrownBy(() -> courseService.addPlace(
                userId,
                courseId,
                AddCoursePlaceRequest.builder()
                        .placeId(placeId)
                        .position(2)
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PLACE_IN_COURSE);

        verify(courseMapper, never()).insertPlace(any());
    }

    @Test
    @DisplayName("내 코스 목록을 content·page·totalElements 로 반환한다")
    void getMyCourses() {
        MyCourseSummaryResponse item = new MyCourseSummaryResponse();
        item.setCourseId(100L);
        item.setName("나의 더현대 코스");
        item.setPlaceCount(3);
        given(courseMapper.findSummariesByUserId(USER_ID, 0, 20)).willReturn(List.of(item));
        given(courseMapper.countByUserId(USER_ID)).willReturn(1L);

        PageResponse<MyCourseSummaryResponse> response = courseService.getMyCourses(USER_ID, 0, 20);

        assertThat(response.getPage()).isZero();
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCourseId()).isEqualTo(100L);
        assertThat(response.getContent().get(0).getName()).isEqualTo("나의 더현대 코스");
        assertThat(response.getContent().get(0).getPlaceCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("size 가 0 이하면 기본 크기(20)로 보정하고 offset 을 page*size 로 계산한다")
    void getMyCoursesClampsPaging() {
        given(courseMapper.findSummariesByUserId(USER_ID, 40, 20)).willReturn(List.of());
        given(courseMapper.countByUserId(USER_ID)).willReturn(0L);

        PageResponse<MyCourseSummaryResponse> response = courseService.getMyCourses(USER_ID, 2, 0);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getContent()).isEmpty();
        verify(courseMapper).findSummariesByUserId(USER_ID, 40, 20);
    }

    @Test
    @DisplayName("본인 소유 코스 상세를 방문 순서와 함께 조회한다")
    void getDetailForOwnCourse() {
        Course course = Course.of(5L, USER_ID, null, "나의 코스", "설명", "MANUAL");
        CoursePlaceResponse firstPlace = new CoursePlaceResponse(
                11L, "템버린즈", "place-picture/74_탬버린즈.jpg", "1F", 1, "향수 브랜드",
                VisitStatus.PENDING.name(), null);
        CoursePlaceResponse secondPlace = new CoursePlaceResponse(
                22L, "설화수", "place-picture/73_설화수.jpg", "1F", 2, "같은 층",
                VisitStatus.VISITED.name(), LocalDateTime.now());
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlacesByCourseId(5L)).willReturn(List.of(firstPlace, secondPlace));
        given(s3Provider.resolveImageUrlByPrefix("place-picture/74_탬버린즈.jpg"))
                .willReturn("https://cdn.example.com/place-picture/74_탬버린즈.jpg");
        given(s3Provider.resolveImageUrlByPrefix("place-picture/73_설화수.jpg"))
                .willReturn("https://cdn.example.com/place-picture/73_설화수.jpg");
        // 대표 사진은 셀럽 사진 쪽 prefix 다 — CDN 이 아니라 버킷 직통으로 나가야 한다.
        given(courseMapper.findHeroImageKey(5L)).willReturn("course/CELE1AA1/1.jpg");
        given(s3Provider.resolveImageUrlByPrefix("course/CELE1AA1/1.jpg"))
                .willReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/course/CELE1AA1/1.jpg");

        CourseDetailResponse response = courseService.getDetail(USER_ID, 5L);

        assertThat(response.getCourseId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("나의 코스");
        assertThat(response.getDescription()).isEqualTo("설명");
        assertThat(response.getCreationType()).isEqualTo("MANUAL");
        assertThat(response.getImageUrl())
                .isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/course/CELE1AA1/1.jpg");
        assertThat(response.getPlaces()).extracting(CoursePlaceResponse::getVisitOrder)
                .containsExactly(1, 2);
        assertThat(response.getPlaces().get(0).getPlaceId()).isEqualTo(11L);
        assertThat(response.getPlaces().get(0).getName()).isEqualTo("템버린즈");
        assertThat(response.getPlaces().get(0).getImageUrl())
                .isEqualTo("https://cdn.example.com/place-picture/74_탬버린즈.jpg");
        assertThat(response.getPlaces().get(0).getFloorCode()).isEqualTo("1F");
        assertThat(response.getPlaces().get(0).getRecommendationReason()).isEqualTo("향수 브랜드");
        assertThat(response.getPlaces().get(0).getVisitStatus()).isEqualTo(VisitStatus.PENDING.name());
        assertThat(response.getPlaces().get(0).getVisitedAt()).isNull();
        verify(courseMapper, never()).existsPublicPostByCourseId(5L);
    }

    @Test
    @DisplayName("사진이 하나도 없는 코스는 대표 사진이 null 이다 (화면이 기본 이미지로 떨어진다)")
    void getDetailWithoutAnyImage() {
        Course course = Course.of(5L, USER_ID, null, "나의 코스", "설명", "MANUAL");
        CoursePlaceResponse place = new CoursePlaceResponse(
                11L, "템버린즈", null, "1F", 1, "향수 브랜드",
                VisitStatus.PENDING.name(), null);
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlacesByCourseId(5L)).willReturn(List.of(place));
        given(courseMapper.findHeroImageKey(5L)).willReturn(null);

        CourseDetailResponse response = courseService.getDetail(USER_ID, 5L);

        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getPlaces()).hasSize(1);
        assertThat(response.getPlaces().get(0).getImageUrl()).isNull();
    }

    @Test
    @DisplayName("코스·장소의 화면용 텍스트를 요청 언어로 번역한다")
    void localizesCourseAndPlaceFields() {
        Course course = Course.of(5L, USER_ID, null, "나의 코스", "설명", "MANUAL");
        CoursePlaceResponse place = new CoursePlaceResponse(
                11L, "템버린즈", null, "1F", 1, "향수 브랜드",
                VisitStatus.PENDING.name(), null);
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlacesByCourseId(5L)).willReturn(List.of(place));
        given(contentTranslationService.translate(
                "course", "5", "name", "나의 코스", ContentLanguage.ENGLISH))
                .willReturn("My Course");
        given(contentTranslationService.translate(
                "course", "5", "description", "설명", ContentLanguage.ENGLISH))
                .willReturn("Description");
        given(contentTranslationService.translate(
                "place", "11", "name", "템버린즈", ContentLanguage.ENGLISH))
                .willReturn("Tamburins");
        given(contentTranslationService.translate(
                "course_place", "5:11", "recommendation_reason", "향수 브랜드", ContentLanguage.ENGLISH))
                .willReturn("Fragrance brand");

        CourseDetailResponse response = courseService.getDetail(USER_ID, 5L, ContentLanguage.ENGLISH);

        assertThat(response.getName()).isEqualTo("My Course");
        assertThat(response.getDescription()).isEqualTo("Description");
        assertThat(response.getPlaces().get(0).getName()).isEqualTo("Tamburins");
        assertThat(response.getPlaces().get(0).getRecommendationReason())
                .isEqualTo("Fragrance brand");
    }

    @Test
    @DisplayName("SYSTEM 기본 코스 상세를 조회한다")
    void getDetailForSystemCourse() {
        Course course = Course.of(3L, null, null, "K-뷰티 코스", "기본 코스", "SYSTEM");
        given(courseMapper.findById(3L)).willReturn(Optional.of(course));
        given(courseMapper.findPlacesByCourseId(3L)).willReturn(List.of());

        CourseDetailResponse response = courseService.getDetail(USER_ID, 3L);

        assertThat(response.getCourseId()).isEqualTo(3L);
        assertThat(response.getCreationType()).isEqualTo(CourseCreationType.SYSTEM.name());
        assertThat(response.getPlaces()).isEmpty();
        verify(courseMapper, never()).existsPublicPostByCourseId(3L);
    }

    @Test
    @DisplayName("유효한 공개 게시글에 연결된 코스 상세를 조회한다")
    void getDetailForCourseLinkedToPublicPost() {
        Course course = Course.of(8L, 99L, null, "커뮤니티 코스", "공개", "MANUAL");
        given(courseMapper.findById(8L)).willReturn(Optional.of(course));
        given(courseMapper.existsPublicPostByCourseId(8L)).willReturn(true);
        given(courseMapper.findPlacesByCourseId(8L)).willReturn(List.of());

        CourseDetailResponse response = courseService.getDetail(USER_ID, 8L);

        assertThat(response.getCourseId()).isEqualTo(8L);
        assertThat(response.getPlaces()).isEmpty();
    }

    @Test
    @DisplayName("없는 코스 상세 조회는 COURSE_NOT_FOUND")
    void rejectGetDetailWhenCourseNotFound() {
        given(courseMapper.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getDetail(USER_ID, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(courseMapper, never()).findPlacesByCourseId(any());
    }

    @Test
    @DisplayName("다른 사용자의 비공개 코스 상세 조회는 거부한다")
    void rejectGetDetailWhenPrivateCourseOwnedByOtherUser() {
        Course course = Course.of(8L, 99L, null, "비공개 코스", null, "MANUAL");
        given(courseMapper.findById(8L)).willReturn(Optional.of(course));
        given(courseMapper.existsPublicPostByCourseId(8L)).willReturn(false);

        assertThatThrownBy(() -> courseService.getDetail(USER_ID, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_OWNER);

        verify(courseMapper, never()).findPlacesByCourseId(any());
    }

    @Test
    @DisplayName("본인 코스를 삭제하면 soft delete 한다")
    void deleteOwnCourse() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));

        courseService.delete(USER_ID, 5L);

        verify(courseMapper).softDelete(5L);
    }

    @Test
    @DisplayName("본인 코스가 아니면 NOT_COURSE_OWNER 로 거부하고 삭제하지 않는다")
    void rejectDeleteWhenNotOwner() {
        Course course = Course.of(5L, 999L, null, "남의 코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.delete(USER_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_OWNER);

        verify(courseMapper, never()).softDelete(any());
    }

    @Test
    @DisplayName("없는 코스를 삭제하면 COURSE_NOT_FOUND")
    void rejectDeleteWhenCourseNotFound() {
        given(courseMapper.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.delete(USER_ID, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(courseMapper, never()).softDelete(any());
    }

    @Test
    @DisplayName("공개 코스를 내 코스로 복사한다")
    void copyPublicCourse() {
        Course sourceCourse = Course.of(3L, null, null, "K-MZ Course", "원본 설명", "SYSTEM");
        given(courseMapper.findById(3L)).willReturn(Optional.of(sourceCourse));
        given(courseMapper.insert(any(CourseInsertCommand.class))).willAnswer(invocation -> {
            CourseInsertCommand command = invocation.getArgument(0);
            command.setCourseId(101L);
            return 1;
        });

        CopyCourseResponse response = courseService.copyPublicCourse(USER_ID, 3L);

        ArgumentCaptor<CourseInsertCommand> captor = ArgumentCaptor.forClass(CourseInsertCommand.class);
        verify(courseMapper).insert(captor.capture());
        CourseInsertCommand saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getSourceCourseId()).isEqualTo(3L);
        assertThat(saved.getName()).isEqualTo("K-MZ Course Copy");
        assertThat(saved.getDescription()).isEqualTo("원본 설명");
        assertThat(saved.getCreationType()).isEqualTo(CourseCreationType.COPIED.name());

        verify(courseMapper, never()).existsPublicPostByCourseId(3L);
        verify(courseMapper).copyPlacesFromCourse(3L, 101L, VisitStatus.PENDING.name());
        assertThat(response.getSourceCourseId()).isEqualTo(3L);
        assertThat(response.getCreatedCourseId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo("K-MZ Course Copy");
    }

    @Test
    @DisplayName("유효한 공개 게시글에 연결된 사용자 코스도 내 코스로 복사한다")
    void copyCourseLinkedToPublicPost() {
        Course sourceCourse = Course.of(3L, 99L, null, "커뮤니티 코스", "공개 게시글 연결", "MANUAL");
        given(courseMapper.findById(3L)).willReturn(Optional.of(sourceCourse));
        given(courseMapper.existsPublicPostByCourseId(3L)).willReturn(true);
        given(courseMapper.insert(any(CourseInsertCommand.class))).willAnswer(invocation -> {
            CourseInsertCommand command = invocation.getArgument(0);
            command.setCourseId(101L);
            return 1;
        });

        CopyCourseResponse response = courseService.copyPublicCourse(USER_ID, 3L);

        ArgumentCaptor<CourseInsertCommand> captor = ArgumentCaptor.forClass(CourseInsertCommand.class);
        verify(courseMapper).insert(captor.capture());
        CourseInsertCommand saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getSourceCourseId()).isEqualTo(3L);
        assertThat(saved.getCreationType()).isEqualTo(CourseCreationType.COPIED.name());
        assertThat(saved.getName()).isEqualTo("커뮤니티 코스 Copy");
        verify(courseMapper).copyPlacesFromCourse(3L, 101L, VisitStatus.PENDING.name());
        assertThat(response.getCreatedCourseId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("존재하지 않는 코스 복사는 COURSE_NOT_FOUND 로 거절한다")
    void rejectCopyWhenSourceCourseNotFound() {
        given(courseMapper.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.copyPublicCourse(USER_ID, 404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(courseMapper, never()).insert(any());
        verify(courseMapper, never()).copyPlacesFromCourse(any(), any(), any());
    }

    @Test
    @DisplayName("사용자 소유 코스는 공개 코스가 아니므로 복사를 거절한다")
    void rejectCopyWhenCourseIsNotPublic() {
        Course sourceCourse = Course.of(3L, 99L, null, "비공개 코스", null, "MANUAL");
        given(courseMapper.findById(3L)).willReturn(Optional.of(sourceCourse));
        given(courseMapper.existsPublicPostByCourseId(3L)).willReturn(false);

        assertThatThrownBy(() -> courseService.copyPublicCourse(USER_ID, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_PUBLIC);

        verify(courseMapper, never()).insert(any());
        verify(courseMapper, never()).copyPlacesFromCourse(any(), any(), any());
    }

    @Test
    @DisplayName("COURSE_PLACE 복사 실패 시 트랜잭션 메서드에서 예외가 전파된다")
    void copyCoursePlaceFailurePropagatesInTransaction() throws NoSuchMethodException {
        Course sourceCourse = Course.of(3L, null, null, "K-MZ Course", null, "SYSTEM");
        given(courseMapper.findById(3L)).willReturn(Optional.of(sourceCourse));
        given(courseMapper.insert(any(CourseInsertCommand.class))).willAnswer(invocation -> {
            CourseInsertCommand command = invocation.getArgument(0);
            command.setCourseId(101L);
            return 1;
        });
        given(courseMapper.copyPlacesFromCourse(3L, 101L, VisitStatus.PENDING.name()))
                .willThrow(new IllegalStateException("copy failed"));

        assertThatThrownBy(() -> courseService.copyPublicCourse(USER_ID, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("copy failed");

        Transactional transactional = CourseService.class
                .getMethod("copyPublicCourse", Long.class, Long.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
    }

    @Test
    @DisplayName("내 코스에서 장소를 삭제하고 뒤쪽 방문 순서를 앞으로 당긴다")
    void deletePlaceFromOwnCourse() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findVisitOrderByCourseAndPlace(5L, 22L)).willReturn(Optional.of(2));

        courseService.deletePlace(USER_ID, 5L, 22L);

        verify(courseMapper).deletePlace(5L, 22L);
        verify(courseMapper).markVisitOrdersAfterDeleted(5L, 2);
        verify(courseMapper).decrementMarkedVisitOrders(5L);
    }

    @Test
    @DisplayName("코스에 없는 장소를 삭제하려 하면 C001 로 거절한다")
    void rejectDeletePlaceWhenPlaceNotInCourse() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findVisitOrderByCourseAndPlace(5L, 999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deletePlace(USER_ID, 5L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(courseMapper, never()).deletePlace(any(), any());
        verify(courseMapper, never()).markVisitOrdersAfterDeleted(any(), org.mockito.Mockito.anyInt());
        verify(courseMapper, never()).decrementMarkedVisitOrders(any());
    }

    @Test
    @DisplayName("본인 코스의 정보와 방문 순서를 수정한다")
    void updateCourse() {
        Course course = Course.of(5L, USER_ID, null, "옛 이름", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlaceIdsByCourseId(5L)).willReturn(List.of(11L, 22L, 33L));

        UpdateCourseResponse response = courseService.update(USER_ID, 5L,
                new UpdateCourseRequest("새 이름", "새 설명", List.of(33L, 11L, 22L)));

        verify(courseMapper).updateInfo(5L, "새 이름", "새 설명");
        verify(courseMapper).markVisitOrdersForReorder(5L);
        verify(courseMapper).reorderPlaces(5L, List.of(33L, 11L, 22L));
        assertThat(response.getCourseId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("새 이름");
        assertThat(response.getOrderedPlaceIds()).containsExactly(33L, 11L, 22L);
    }

    @Test
    @DisplayName("name·description 을 생략하면 기존 값을 유지한다(부분 수정)")
    void updateKeepsInfoWhenOmitted() {
        Course course = Course.of(5L, USER_ID, null, "기존 이름", "기존 설명", "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlaceIdsByCourseId(5L)).willReturn(List.of(11L, 22L));

        UpdateCourseResponse response = courseService.update(USER_ID, 5L,
                new UpdateCourseRequest(null, null, List.of(22L, 11L)));

        verify(courseMapper).updateInfo(5L, "기존 이름", "기존 설명");
        verify(courseMapper).markVisitOrdersForReorder(5L);
        verify(courseMapper).reorderPlaces(5L, List.of(22L, 11L));
        assertThat(response.getName()).isEqualTo("기존 이름");
        assertThat(response.getOrderedPlaceIds()).containsExactly(22L, 11L);
    }

    @Test
    @DisplayName("본인 코스가 아니면 NOT_COURSE_OWNER 로 거부하고 수정하지 않는다")
    void rejectUpdateWhenNotOwner() {
        Course course = Course.of(5L, 999L, null, "남의 코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.update(USER_ID, 5L,
                new UpdateCourseRequest("새 이름", null, List.of(1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_OWNER);

        verify(courseMapper, never()).updateInfo(any(), any(), any());
        verify(courseMapper, never()).reorderPlaces(any(), any());
    }

    @Test
    @DisplayName("코스에 속한 장소 집합과 다르면(누락·추가) 순서 수정을 거절한다")
    void rejectUpdateWhenPlaceSetMismatch() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlaceIdsByCourseId(5L)).willReturn(List.of(11L, 22L, 33L));

        assertThatThrownBy(() -> courseService.update(USER_ID, 5L,
                new UpdateCourseRequest("새 이름", null, List.of(11L, 22L))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(courseMapper, never()).updateInfo(any(), any(), any());
        verify(courseMapper, never()).reorderPlaces(any(), any());
    }
}
