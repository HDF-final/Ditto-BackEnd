package com.ditto.news.service.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ditto.news.pipeline.model.NewsArticleCandidate;

import lombok.extern.slf4j.Slf4j;

/**
 * 수집된 기사 후보 목록을 K-컬처 대상 주제/키워드로 1차 필터링하는 컴포넌트.
 * 제목 또는 본문 요약(description)에 허용된 K-POP 키워드/아티스트가 포함된 기사만 통과시키며 매칭된 키워드를 기록합니다.
 * "문화", "공연", "예술", "축제", "음악", "엔터테인먼트" 등 지나치게 넓은 일반 단어는 제외합니다.
 */
@Slf4j
@Component
public class NewsKeywordFilter {

    /** K-POP 관련성 판단을 위한 정밀 키워드 목록 */
    private static final List<String> KPOP_KEYWORDS = List.of(
            "k-pop", "kpop", "k pop", "케이팝",
            "idol", "아이돌", "boy group", "girl group", "boy band", "girl band",
            "보이그룹", "걸그룹",
            "bts", "blackpink", "black pink", "newjeans", "new jeans", "aespa", "seventeen", "ive",
            "stray kids", "straykids", "twice", "le sserafim", "lesserafim", "txt", "riize", "nct",
            "enhypen", "bigbang", "zerobaseone", "boynextdoor",
            "방탄소년단", "뉴진스", "에스파", "세븐틴", "아이브", "스트레이 키즈", "스트레이키즈", "스키즈",
            "트와이스", "르세라핌", "라이즈", "엔시티", "투모로우바이투게더", "엔하이픈", "빅뱅"
    );

    private static final Map<String, List<String>> TOPIC_KEYWORDS_MAP = Map.of(
            "k-pop", KPOP_KEYWORDS
    );

    /**
     * 기사 후보 목록을 키워드 목록으로 1차 필터링합니다.
     *
     * @param candidates 기사 후보 목록
     * @param keywords   필터링할 K-컬처 주제 목록 (예: K-POP)
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

            for (String topic : validKeywords) {
                List<String> termsToMatch = getTopicSearchTerms(topic);
                boolean matched = false;

                for (String term : termsToMatch) {
                    String lowerTerm = term.toLowerCase(Locale.ROOT);
                    if (title.contains(lowerTerm) || description.contains(lowerTerm)) {
                        NewsArticleCandidate candidateWithTopic = NewsArticleCandidate.builder()
                                .title(candidate.getTitle())
                                .url(candidate.getUrl())
                                .source(candidate.getSource())
                                .publishedAt(candidate.getPublishedAt())
                                .description(candidate.getDescription())
                                .matchedKeyword(topic)
                                .build();
                        filteredList.add(candidateWithTopic);
                        matched = true;
                        break; // 이 candidate에 대해 중복 추가 방지
                    }
                }

                if (matched) {
                    break;
                }
            }
        }

        return filteredList;
    }

    private List<String> getTopicSearchTerms(String topic) {
        String lowerTopic = topic.trim().toLowerCase(Locale.ROOT);
        List<String> mapped = TOPIC_KEYWORDS_MAP.get(lowerTopic);
        if (mapped != null) {
            return mapped;
        }
        return List.of(lowerTopic);
    }
}
