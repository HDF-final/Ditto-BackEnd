package com.ditto.news.service.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ditto.news.pipeline.model.NewsArticleCandidate;

import lombok.extern.slf4j.Slf4j;

/**
 * 수집된 기사 후보 목록을 K-컬처 대상 주제/키워드로 1차 필터링하는 컴포넌트.
 * 제목 또는 본문 요약(description)에 키워드가 포함된 기사만 통과시키며 매칭된 키워드를 기록합니다.
 */
@Slf4j
@Component
public class NewsKeywordFilter {

    /**
     * 기사 후보 목록을 키워드 목록으로 1차 필터링합니다.
     *
     * @param candidates 기사 후보 목록
     * @param keywords   필터링할 K-컬처 주제 목록 (예: K-POP, K-뷰티, 서울 핫플 등)
     * @return 필터링된 기사 후보 목록
     */
    public List<NewsArticleCandidate> filterByKeywords(List<NewsArticleCandidate> candidates, List<String> keywords) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        // 유효한 키워드만 정제 (null, 빈 문자열, 공백 제외)
        List<String> validKeywords = keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .toList();

        if (validKeywords.isEmpty()) {
            log.debug("유효한 필터링 키워드가 없어 전체 후보를 제외합니다.");
            return Collections.emptyList();
        }

        List<NewsArticleCandidate> filteredList = new ArrayList<>();

        for (NewsArticleCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            String title = candidate.getTitle() != null ? candidate.getTitle().toLowerCase(Locale.ROOT) : "";
            String description = candidate.getDescription() != null ? candidate.getDescription().toLowerCase(Locale.ROOT) : "";

            for (String keyword : validKeywords) {
                String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
                if (title.contains(normalizedKeyword) || description.contains(normalizedKeyword)) {
                    NewsArticleCandidate matched = NewsArticleCandidate.builder()
                            .title(candidate.getTitle())
                            .url(candidate.getUrl())
                            .source(candidate.getSource())
                            .publishedAt(candidate.getPublishedAt())
                            .description(candidate.getDescription())
                            .matchedKeyword(keyword)
                            .build();
                    filteredList.add(matched);
                    break; // 중복 추가 방지
                }
            }
        }

        return filteredList;
    }
}
