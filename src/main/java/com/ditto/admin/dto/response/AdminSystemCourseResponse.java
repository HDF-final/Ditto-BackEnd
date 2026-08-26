package com.ditto.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 어드민 "기본 추천 코스" 한 줄.
 *
 * <p>목록과 상세가 같은 모양이다 — 상세에만 {@code places} 가 찬다. 화면이 두 가지
 * 모양을 알 이유가 없다.
 */
@Getter
@Builder
public class AdminSystemCourseResponse {

    private Long courseId;
    private String name;
    private String description;
    private String countryCode;
    private String shareCode;
    private int placeCount;
    private Long postId;
    private String postContent;
    private int imageCount;

    /**
     * 목록 카드의 대표 사진. <b>첫 자리의 매장 사진</b>이고 손님 목록이 쓰는 것과 같다.
     * 화면이 그대로 {@code <img src>} 로 쓰게 S3 주소로 풀어서 준다. 자리가 없으면 null 이고
     * 그때 카드는 인물 이름 두 글자를 대신 띄운다.
     */
    private String heroImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 어느 인물의 코스인가. 이 경로로 올린 것만 채워진다 — 그 전에 만든 코스(1번·122번)는
     * null 이다.
     */
    private String celebrity;

    /**
     * {@code queued} · {@code running} · {@code done} · {@code failed}.
     * 반영 기록이 없는 코스는 {@code done} 으로 본다 — 걸려 있는 것은 사실이다.
     */
    private String state;

    /** 사람이 읽는 진행 문구. {@code "문안을 쓰는 중"} · {@code "진행완료"}. */
    private String step;

    private String error;
    private List<String> warnings;

    /** 상세에서만 찬다. */
    private List<AdminSystemCoursePlaceResponse> places;
}
