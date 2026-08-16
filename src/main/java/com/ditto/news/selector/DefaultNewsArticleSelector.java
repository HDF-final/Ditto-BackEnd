package com.ditto.news.selector;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ditto.news.pipeline.NewsArticleSelector;
import com.ditto.news.pipeline.model.CrawledNewsArticle;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link NewsArticleSelector} 파이프라인 인터페이스 구현체.
 * 크롤링된 뉴스 기사 목록에서 주제 관련성 점수 계산, URL 및 정규화 제목 중복 제거,
 * 기사 최신성 검증 및 상위 N개 선별을 deterministic 규칙에 따라 수행합니다.
 */
@Slf4j
@Service
public class DefaultNewsArticleSelector implements NewsArticleSelector {

    /** 최소 요구 관련성 점수 (미달 시 관련 기사에서 제외) */
    public static final int MIN_RELEVANCE_SCORE = 30;

    /** 제목 키워드 직접 일치 점수 */
    public static final int TITLE_EXACT_MATCH_SCORE = 100;

    /** 본문 키워드 직접 일치 기본 점수 */
    public static final int BODY_EXACT_MATCH_SCORE = 40;

    /** 제목 연관 보조 표현 일치 점수 */
    public static final int TITLE_RELATED_TERM_SCORE = 50;

    /** 본문 연관 보조 표현 일치 기본 점수 */
    public static final int BODY_RELATED_TERM_SCORE = 20;

    /** 최신성 보너스 점수 (24시간 이내 발행) */
    public static final int RECENCY_24H_SCORE = 15;

    /** 최신성 보너스 점수 (72시간 이내 발행) */
    public static final int RECENCY_72H_SCORE = 10;

    /** 최신성 보너스 점수 (7일 이내 발행) */
    public static final int RECENCY_7D_SCORE = 5;

    /** 기사 최대 유효 기간 (일 단위): 14일 초과 기사는 제외 */
    public static final int MAX_ARTICLE_AGE_DAYS = 14;

    /** 최종 선별할 최대 기사 수 */
    public static final int MAX_SELECTED_ARTICLES = 5;

    /** K-POP 관련성 판단을 위한 정밀 키워드/아티스트 목록 */
    private static final List<String> KPOP_RELATED_TERMS = List.of(
            "k-pop", "kpop", "k pop", "케이팝",
            "idol", "아이돌", "boy group", "girl group", "boy band", "girl band",
            "보이그룹", "걸그룹",
            "bts", "blackpink", "black pink", "newjeans", "new jeans", "aespa", "seventeen", "ive",
            "stray kids", "straykids", "twice", "le sserafim", "lesserafim", "txt", "riize", "nct",
            "enhypen", "bigbang", "zerobaseone", "boynextdoor",
            "방탄소년단", "뉴진스", "에스파", "세븐틴", "아이브", "스트레이 키즈", "스트레이키즈", "스키즈",
            "트와이스", "르세라핌", "라이즈", "엔시티", "투모로우바이투게더", "엔하이픈", "빅뱅"
    );

    /** K-POP 토픽 보조 표현 맵 */
    private static final Map<String, List<String>> RELATED_TERMS_MAP = Map.of(
            "k-pop", KPOP_RELATED_TERMS
    );

    private final Clock clock;

    public DefaultNewsArticleSelector() {
        this(Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public DefaultNewsArticleSelector(Clock clock) {
        this.clock = clock != null ? clock : Clock.system(ZoneId.of("Asia/Seoul"));
    }

    /**
     * 대상 키워드 목록을 기반으로 크롤링된 기사 목록에서 연관성이 높고 중복되지 않은 기사들을 선별합니다.
     *
     * @param articles       크롤링된 기사 목록
     * @param targetKeywords 선별 기준 키워드 목록 (예: ["K-POP"])
     * @return 관련성 내림차순 및 최신순으로 정렬된 상위 기사 목록 (최대 5개)
     */
    @Override
    public List<CrawledNewsArticle> selectRelevantArticles(List<CrawledNewsArticle> articles, List<String> targetKeywords) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }
        if (targetKeywords == null || targetKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> validKeywords = targetKeywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .toList();

        if (validKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoffDate = now.minusDays(MAX_ARTICLE_AGE_DAYS);

        // 1. 유효성 검사 및 오래된 기사 제외
        List<CrawledNewsArticle> validArticles = articles.stream()
                .filter(a -> a != null
                        && a.getUrl() != null && !a.getUrl().isBlank()
                        && a.getTitle() != null && !a.getTitle().isBlank()
                        && a.getBody() != null && !a.getBody().isBlank())
                .filter(a -> a.getPublishedAt() == null || !a.getPublishedAt().isBefore(cutoffDate))
                .toList();

        if (validArticles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 관련성 점수 계산 및 무관 기사 제외
        List<ScoredArticle> scoredArticles = new ArrayList<>();
        for (CrawledNewsArticle article : validArticles) {
            int score = calculateRelevanceScore(article, validKeywords, now);
            if (score >= MIN_RELEVANCE_SCORE) {
                scoredArticles.add(new ScoredArticle(article, score));
            }
        }

        if (scoredArticles.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 중복 제거 (URL 및 정규화 제목 기준, 고득점/최신순 우선 보존)
        List<ScoredArticle> deduplicated = deduplicateArticles(scoredArticles);

        // 4. 최대 N개 제한 및 최종 기사 리스트 반환
        return deduplicated.stream()
                .limit(MAX_SELECTED_ARTICLES)
                .map(ScoredArticle::getArticle)
                .toList();
    }

    /**
     * 단일 키워드에 대한 기사 선별 편의 메서드.
     */
    public List<CrawledNewsArticle> selectRelevantArticlesForKeyword(List<CrawledNewsArticle> articles, String targetKeyword) {
        if (targetKeyword == null || targetKeyword.isBlank()) {
            return Collections.emptyList();
        }
        return selectRelevantArticles(articles, List.of(targetKeyword.trim()));
    }

    /**
     * 기사의 제목, 본문, 보조 표현, 최신성을 종합하여 관련성 점수를 계산합니다.
     */
    public int calculateRelevanceScore(CrawledNewsArticle article, List<String> targetKeywords, LocalDateTime now) {
        String title = article.getTitle() != null ? article.getTitle().toLowerCase(Locale.ROOT) : "";
        String body = article.getBody() != null ? article.getBody().toLowerCase(Locale.ROOT) : "";

        boolean hasDirectTitleMatch = false;
        boolean hasDirectBodyMatch = false;
        boolean hasRelatedTitleMatch = false;
        boolean hasRelatedBodyMatch = false;

        for (String keyword : targetKeywords) {
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            if (title.contains(lowerKeyword)) {
                hasDirectTitleMatch = true;
            }
            if (body.contains(lowerKeyword)) {
                hasDirectBodyMatch = true;
            }

            // 보조 연관 키워드 검사
            List<String> relatedTerms = getRelatedTerms(keyword);
            for (String term : relatedTerms) {
                String lowerTerm = term.toLowerCase(Locale.ROOT);
                if (title.contains(lowerTerm)) {
                    hasRelatedTitleMatch = true;
                }
                if (body.contains(lowerTerm)) {
                    hasRelatedBodyMatch = true;
                }
            }
        }

        // 키워드나 연관어가 제목/본문 어디에도 없으면 0점 (무관 기사)
        if (!hasDirectTitleMatch && !hasDirectBodyMatch && !hasRelatedTitleMatch && !hasRelatedBodyMatch) {
            return 0;
        }

        int score = 0;
        if (hasDirectTitleMatch) {
            score += TITLE_EXACT_MATCH_SCORE;
        }
        if (hasDirectBodyMatch) {
            score += BODY_EXACT_MATCH_SCORE;
        }
        if (hasRelatedTitleMatch) {
            score += TITLE_RELATED_TERM_SCORE;
        }
        if (hasRelatedBodyMatch) {
            score += BODY_RELATED_TERM_SCORE;
        }

        // 최신성 보너스 점수 가산
        if (article.getPublishedAt() != null) {
            Duration age = Duration.between(article.getPublishedAt(), now);
            if (age.isNegative() || age.toHours() <= 24) {
                score += RECENCY_24H_SCORE;
            } else if (age.toHours() <= 72) {
                score += RECENCY_72H_SCORE;
            } else if (age.toDays() <= 7) {
                score += RECENCY_7D_SCORE;
            }
        }

        return score;
    }

    /**
     * 중복 감지를 위해 기사 제목을 정규화합니다.
     * 소문자 변환, 대괄호/소괄호 접두사 제거, 특수문자 제거, 연속 공백 정리, K-Culture 단어 표기 통일.
     */
    public String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String normalized = title.toLowerCase(Locale.ROOT);

        // 1. [단독], [속보], [포토], (종합), [Exclusive] 등 괄호 태그 제거
        normalized = normalized.replaceAll("\\[[^\\]]*\\]|\\([^\\)]*\\)", " ");

        // 2. K-POP / k pop / kpop 등 표기 통일
        normalized = normalized.replaceAll("k[\\s-_]*pop", "kpop");
        normalized = normalized.replaceAll("k[\\s-_]*beauty", "kbeauty");
        normalized = normalized.replaceAll("k[\\s-_]*fashion", "kfashion");

        // 3. 특수문자 제거 (한글, 영문, 숫자, 공백만 유지)
        normalized = normalized.replaceAll("[^a-z0-9가-힣\\s]", " ");

        // 4. 연속 공백 정리 및 앞뒤 공백 제거
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /**
     * URL 및 정규화된 제목 기준으로 중복을 제거합니다.
     * 점수가 더 높고 최신인 기사를 우선적으로 유지합니다.
     */
    private List<ScoredArticle> deduplicateArticles(List<ScoredArticle> scoredArticles) {
        // 점수 내림차순, 최신순(nulls last)으로 정렬
        List<ScoredArticle> sorted = scoredArticles.stream()
                .sorted(Comparator
                        .comparingInt(ScoredArticle::getScore).reversed()
                        .thenComparing(ScoredArticle::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ScoredArticle> deduplicated = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();

        for (ScoredArticle item : sorted) {
            String url = item.getArticle().getUrl() != null ? item.getArticle().getUrl().trim() : "";
            String normalizedTitle = normalizeTitle(item.getArticle().getTitle());

            boolean duplicateUrl = !url.isEmpty() && seenUrls.contains(url);
            boolean duplicateTitle = !normalizedTitle.isEmpty() && seenTitles.contains(normalizedTitle);

            if (!duplicateUrl && !duplicateTitle) {
                if (!url.isEmpty()) seenUrls.add(url);
                if (!normalizedTitle.isEmpty()) seenTitles.add(normalizedTitle);
                deduplicated.add(item);
            }
        }

        return deduplicated;
    }

    private List<String> getRelatedTerms(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }
        String lower = keyword.trim().toLowerCase(Locale.ROOT);
        return RELATED_TERMS_MAP.getOrDefault(lower, Collections.emptyList());
    }

    /**
     * 점수 및 정렬용 내부 래퍼 클래스.
     */
    private static class ScoredArticle {
        private final CrawledNewsArticle article;
        private final int score;

        public ScoredArticle(CrawledNewsArticle article, int score) {
            this.article = article;
            this.score = score;
        }

        public CrawledNewsArticle getArticle() {
            return article;
        }

        public int getScore() {
            return score;
        }

        public LocalDateTime getPublishedAt() {
            return article.getPublishedAt();
        }
    }
}
