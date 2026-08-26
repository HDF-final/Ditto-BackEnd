package com.ditto.admin.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기본 추천 코스 수정. <b>보낸 칸만 고친다</b> — null 은 "그대로 둬라" 다.
 *
 * <p>자리 구성(어느 매장을 몇 번째로)은 여기서 안 바꾼다. 그건 셀럽 편집기에서 고쳐
 * 다시 승인하면 덮어쓰는 길이 이미 있고, 두 곳에서 같은 일을 하면 어느 쪽이 최신인지
 * 알 수 없게 된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminSystemCourseUpdateRequest {

    /** {@code COURSE.NAME} 은 VARCHAR2(100) 이다. */
    @Size(max = 100, message = "코스 이름은 100자를 넘을 수 없습니다.")
    private String name;

    private String description;

    /** {@code COUNTRY.CODE} 와 같은 글자 (KR·JP·CN·US). 빈 문자열이면 나라를 지운다. */
    @Size(max = 10, message = "국가 코드는 10자를 넘을 수 없습니다.")
    private String countryCode;

    /** 커뮤니티 글 본문이 아니라 코스 소개 문안이다. 추천 리스트 카드가 이걸 쓴다. */
    private String postContent;

    private List<PlaceReason> places;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlaceReason {
        private Long placeId;
        private String recommendationReason;
    }
}
