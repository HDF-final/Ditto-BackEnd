package com.ditto.admin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 관리자 화면에서 조회할 수 있는 트렌드 산출물의 고정 목록. */
@Getter
@RequiredArgsConstructor
public enum TrendArtifactType {

    TOP4("top4", "국가별 TOP 4", "trends/country-ranking/latest-top4.json"),
    CANDIDATES("candidates", "국가별 후보 TOP 20", "trends/country-ranking/latest-candidates.json"),
    YOUTUBE("youtube", "YouTube 급상승 TOP 10", "trends/youtube/latest-top10.json");

    private final String code;
    private final String displayName;
    private final String objectKey;
}
