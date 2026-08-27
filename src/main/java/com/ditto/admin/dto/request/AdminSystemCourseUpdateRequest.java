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

    /**
     * 이 코스를 걸 나라들. {@code COUNTRY.CODE} 와 같은 글자 (KR·JP·CN·US).
     *
     * <p><b>빈 배열이면 나라를 지운다</b> — null("그대로 둬라")과 뜻이 다르다. 나라가
     * 없는 코스는 손님 화면의 어느 나라 버튼에서도 안 보이니, 지우는 것은 사실상
     * 내리는 것이다.
     */
    @Size(max = 20, message = "나라는 20개를 넘을 수 없습니다.")
    private List<@Size(max = 10, message = "국가 코드는 10자를 넘을 수 없습니다.") String> countryCodes;

    /**
     * 대표 사진의 S3 키. <b>빈 문자열이 "기본값으로 되돌린다"</b> 는 뜻이고, null 은
     * 여기서도 "그대로 둬라" 다.
     *
     * <p>키를 받는 이유는 이 창구가 사진을 받아 올리지 않기 때문이다 — 화면은 이 코스에
     * 이미 붙어 있는 사진(자리 사진·셀럽 사진) 중에서 고른다. 새 사진을 넣는 것은 승인
     * 편집기에서 하고, 그건 반영 람다가 받아 올린다.
     */
    @Size(max = 500, message = "대표 사진 키는 500자를 넘을 수 없습니다.")
    private String mainImage;

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
