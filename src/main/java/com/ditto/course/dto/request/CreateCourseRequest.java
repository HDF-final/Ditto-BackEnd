package com.ditto.course.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내 코스 생성 요청.
 * 수동 모드 "빈 코스로 시작하기"는 name/placeIds 없이 호출한다.
 * placeIds 가 있으면 DB {@code place} 에 존재하는 ID 만 허용한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

    @Size(max = 100)
    private String name;

    private String description;

    private List<Long> placeIds;
}
