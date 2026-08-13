package com.ditto.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.CourseMapper.CoursePlaceInsertCommand;
import com.ditto.course.repository.PlaceMapper;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

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
}
