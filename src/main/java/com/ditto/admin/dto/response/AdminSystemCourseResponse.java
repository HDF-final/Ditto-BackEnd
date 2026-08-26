package com.ditto.admin.dto.response;

import java.time.OffsetDateTime;
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
    /**
     * 이 코스가 걸린 나라들. DB 에는 {@code 'KR,JP'} 처럼 한 칸에 쉼표로 들어 있는데,
     * 화면이 칩을 하나씩 그리므로 갈라서 준다.
     */
    private List<String> countryCodes;
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

    /**
     * 관리자가 <b>직접 지정한</b> 대표 사진의 S3 키. 안 지정했으면 null 이고, 그때
     * {@code heroImageUrl} 은 기본값(셀럽 사진 → 첫 자리 매장 사진)으로 채워져 온다.
     *
     * <p>둘 다 주는 것은 편집기가 "지금 기본값을 쓰는 중" 인지 알아야 <b>기본값으로
     * 되돌리기</b>를 그릴 수 있기 때문이다.
     */
    private String mainImage;

    /** 위 키를 바로 쓸 수 있는 주소로 푼 것. 지정 안 했으면 null. */
    private String mainImageUrl;

    /**
     * 올린 때 · 마지막 수정. <b>시간대를 달고 나간다.</b>
     *
     * <p>DB 가 {@code SYSTIMESTAMP} 로 적는데 그 서버 시계가 <b>UTC</b> 다. {@code LocalDateTime}
     * 으로 내보내면 칸 없는 문자열({@code "2026-08-26T07:00:02"})이 되고, 브라우저는 그걸
     * 제 시간대로 읽는다 — 한국에서 보면 아홉 시간 이른 시각이 뜬다. 실제로 그렇게 떴다.
     *
     * <p>여기서 UTC 라고 못 박아 보내면 받는 쪽이 어느 시간대로 그리든 같은 순간을 가리킨다.
     */
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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
