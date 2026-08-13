package com.ditto.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.course.domain.CourseCreationType;
import com.ditto.course.domain.VisitStatus;
import com.ditto.course.domain.Course;
import com.ditto.course.dto.request.CreateCourseRequest;
import com.ditto.course.dto.response.CreateCourseResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CourseInsertCommand;
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

    @BeforeEach
    void stubInsertCourseId() {
        lenient().when(courseMapper.insert(any(CourseInsertCommand.class))).thenAnswer(invocation -> {
            CourseInsertCommand command = invocation.getArgument(0);
            command.setCourseId(101L);
            return 1;
        });
        lenient().when(courseMapper.countByShareCode(anyString())).thenReturn(0);
    }

    @Test
    @DisplayName("장소 없이 호출하면 빈 코스로 저장한다")
    void createEmptyCourse() {
        CreateCourseResponse response = courseService.create(
                USER_ID, new CreateCourseRequest(null, null, null));

        ArgumentCaptor<CourseInsertCommand> captor = ArgumentCaptor.forClass(CourseInsertCommand.class);
        verify(courseMapper).insert(captor.capture());
        verify(courseMapper, never()).insertPlace(any());
        verify(placeMapper, never()).findExistingIds(any());

        CourseInsertCommand saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getSourceCourseId()).isNull();
        assertThat(saved.getName()).isEqualTo(CourseService.DEFAULT_COURSE_TITLE);
        assertThat(saved.getCreationType()).isEqualTo(CourseCreationType.MANUAL.name());
        assertThat(saved.getShareCode()).isNotBlank();
        assertThat(response.getCourseId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo(CourseService.DEFAULT_COURSE_TITLE);
        assertThat(response.getPlaces()).isEmpty();
    }

    @Test
    @DisplayName("place 테이블에 있는 장소만 방문 순서대로 담는다")
    void createCourseWithExistingPlaces() {
        given(placeMapper.findExistingIds(List.of(3L, 1L))).willReturn(List.of(3L, 1L));

        CreateCourseResponse response = courseService.create(
                USER_ID, new CreateCourseRequest("K-뷰티 코스", "1층부터", List.of(3L, 1L)));

        ArgumentCaptor<CoursePlaceInsertCommand> placeCaptor =
                ArgumentCaptor.forClass(CoursePlaceInsertCommand.class);
        verify(courseMapper, org.mockito.Mockito.times(2)).insertPlace(placeCaptor.capture());

        List<CoursePlaceInsertCommand> inserted = placeCaptor.getAllValues();
        assertThat(inserted.get(0).getPlaceId()).isEqualTo(3L);
        assertThat(inserted.get(0).getVisitOrder()).isEqualTo(1);
        assertThat(inserted.get(0).getVisitStatus()).isEqualTo(VisitStatus.PENDING.name());
        assertThat(inserted.get(1).getPlaceId()).isEqualTo(1L);
        assertThat(inserted.get(1).getVisitOrder()).isEqualTo(2);

        assertThat(response.getName()).isEqualTo("K-뷰티 코스");
        assertThat(response.getPlaces()).extracting("placeId").containsExactly(3L, 1L);
        assertThat(response.getPlaces()).extracting("order").containsExactly(1, 2);
    }

    @Test
    @DisplayName("place 테이블에 없는 장소 ID는 거절한다")
    void rejectUnknownPlace() {
        given(placeMapper.findExistingIds(List.of(999L))).willReturn(List.of());

        assertThatThrownBy(() -> courseService.create(
                USER_ID, new CreateCourseRequest("코스", null, List.of(999L))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_NOT_FOUND);

        verify(courseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("같은 장소를 두 번 넣으면 거절한다")
    void rejectDuplicatePlace() {
        assertThatThrownBy(() -> courseService.create(
                USER_ID, new CreateCourseRequest("코스", null, List.of(1L, 1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PLACE_IN_COURSE);
    }

    @Test
    @DisplayName("없는 코스를 조회하면 COURSE_NOT_FOUND")
    void requireCourseNotFound() {
        given(courseMapper.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.requireCourse(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("코스 소유자는 isOwnedBy 로 판별한다")
    void courseOwnership() {
        Course course = Course.of(1L, USER_ID, null, "코스", null, "MANUAL", "ABCD1234");

        assertThat(course.isOwnedBy(USER_ID)).isTrue();
        assertThat(course.isOwnedBy(99L)).isFalse();
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
}
