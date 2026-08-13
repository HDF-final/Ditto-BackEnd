package com.ditto.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.course.domain.Course;
import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.domain.VisitStatus;
import com.ditto.course.dto.request.AddCoursePlaceRequest;
import com.ditto.course.dto.response.AddCoursePlaceResponse;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.request.UpdateCourseRequest;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.dto.response.UpdateCourseResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CoursePlaceInsertCommand;
import com.ditto.course.repository.PlaceMapper;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private PlaceMapper placeMapper;

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
                        CourseCreationType.MANUAL.name(),
                        "ABCDEFGH")));
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
                        CourseCreationType.MANUAL.name(),
                        "ABCDEFGH")));
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
    @DisplayName("본인 코스를 삭제하면 soft delete 한다")
    void deleteOwnCourse() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL", "ABCD1234");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));

        courseService.delete(USER_ID, 5L);

        verify(courseMapper).softDelete(5L);
    }

    @Test
    @DisplayName("본인 코스가 아니면 NOT_COURSE_OWNER 로 거부하고 삭제하지 않는다")
    void rejectDeleteWhenNotOwner() {
        Course course = Course.of(5L, 999L, null, "남의 코스", null, "MANUAL", "ABCD1234");
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
    @DisplayName("내 코스에서 장소를 삭제하고 뒤쪽 방문 순서를 앞으로 당긴다")
    void deletePlaceFromOwnCourse() {
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL", "ABCD1234");
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
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL", "ABCD1234");
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
        Course course = Course.of(5L, USER_ID, null, "옛 이름", null, "MANUAL", "ABCD1234");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlaceIdsByCourseId(5L)).willReturn(List.of(11L, 22L, 33L));

        UpdateCourseResponse response = courseService.update(USER_ID, 5L,
                new UpdateCourseRequest("새 이름", "새 설명", List.of(33L, 11L, 22L)));

        verify(courseMapper).updateInfo(5L, "새 이름", "새 설명");
        verify(courseMapper).reorderPlaces(5L, List.of(33L, 11L, 22L));
        assertThat(response.getCourseId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("새 이름");
        assertThat(response.getOrderedPlaceIds()).containsExactly(33L, 11L, 22L);
    }

    @Test
    @DisplayName("name·description 을 생략하면 기존 값을 유지한다(부분 수정)")
    void updateKeepsInfoWhenOmitted() {
        Course course = Course.of(5L, USER_ID, null, "기존 이름", "기존 설명", "MANUAL", "ABCD1234");
        given(courseMapper.findById(5L)).willReturn(Optional.of(course));
        given(courseMapper.findPlaceIdsByCourseId(5L)).willReturn(List.of(11L, 22L));

        UpdateCourseResponse response = courseService.update(USER_ID, 5L,
                new UpdateCourseRequest(null, null, List.of(22L, 11L)));

        verify(courseMapper).updateInfo(5L, "기존 이름", "기존 설명");
        assertThat(response.getName()).isEqualTo("기존 이름");
        assertThat(response.getOrderedPlaceIds()).containsExactly(22L, 11L);
    }

    @Test
    @DisplayName("본인 코스가 아니면 NOT_COURSE_OWNER 로 거부하고 수정하지 않는다")
    void rejectUpdateWhenNotOwner() {
        Course course = Course.of(5L, 999L, null, "남의 코스", null, "MANUAL", "ABCD1234");
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
        Course course = Course.of(5L, USER_ID, null, "코스", null, "MANUAL", "ABCD1234");
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
