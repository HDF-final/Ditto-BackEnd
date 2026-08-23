package com.ditto.news.outbound.selector;

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

import com.ditto.news.application.port.out.NewsArticleSelector;
import com.ditto.news.domain.CrawledNewsArticle;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link NewsArticleSelector} 포트 구현체.
 * 크롤링된 뉴스 기사 목록에서 주제 관련성 점수 계산, 동일 아티스트/토픽 엔티티 중복 방지,
 * 자카드(Jaccard) 어휘 유사도 기반 동일 사건 중복 제거, 기사 최신성 검증 및 상위 N개 선별을 수행합니다.
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

    /** 음악/앨범/컴백/투어 등 핵심 K-POP 활동 가산 키워드 */
    private static final List<String> CORE_MUSIC_KEYWORDS = List.of(
            "[가요소식]", "[가요]", "음반", "음원", "빌보드", "billboard", "컴백", "comeback",
            "밀리언셀러", "스트리밍", "streaming", "앨범", "album", "차트", "chart", "쇼케이스",
            "콘서트", "concert", "월드투어", "world tour", "투어", "팬미팅", "fan meeting",
            "뮤직비디오", "뮤비", "mv", "수록곡", "신곡", "스포티파이", "spotify", "오리콘", "oricon",
            "싱글", "single", "신보", "발매", "트랙", "track", "1위", "톱10", "top10"
    );

    /** K-POP과 무관한 기술/스포츠/정치/사회 노이즈 감점 키워드 */
    private static final List<String> NON_KPOP_NOISE_KEYWORDS = List.of(
            "로봇", "robot", "피지컬 ai", "엔터테크", "로봇파크", "기념메달", "주화", "화폐",
            "손흥민", "조수미", "페이커", "축구", "야구", "골프", "올림픽", "정치", "국회",
            "대통령", "총선", "주식", "증시", "코인", "재판", "법원", "검찰", "구속", "사기", "음주운전"
    );

    /** 음악 활동 핵심 키워드 일치 가산점 */
    public static final int MUSIC_ACTIVITY_BONUS = 60;

    /** 비관련 노이즈 키워드 감점 */
    public static final int NOISE_PENALTY = 150;

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

    /** 제목 자카드 유사도 중복 임계치 (60% 이상 단어 일치 시 동일 사건으로 판단) */
    public static final double SIMILARITY_THRESHOLD = 0.60;

    /** K-POP 대표 아티스트 정규화 엔티티 매핑 (동일 아티스트 뉴스 중복 선별 방지) */
    private static final Map<String, String> ARTIST_CANONICAL_MAP = Map.ofEntries(
            Map.entry("스트레이 키즈", "stray_kids"),
            Map.entry("스트레이키즈", "stray_kids"),
            Map.entry("스키즈", "stray_kids"),
            Map.entry("stray kids", "stray_kids"),
            Map.entry("straykids", "stray_kids"),
            Map.entry("방탄소년단", "bts"),
            Map.entry("bts", "bts"),
            Map.entry("뉴진스", "newjeans"),
            Map.entry("newjeans", "newjeans"),
            Map.entry("new jeans", "newjeans"),
            Map.entry("에스파", "aespa"),
            Map.entry("aespa", "aespa"),
            Map.entry("세븐틴", "seventeen"),
            Map.entry("seventeen", "seventeen"),
            Map.entry("아이브", "ive"),
            Map.entry("ive", "ive"),
            Map.entry("트와이스", "twice"),
            Map.entry("twice", "twice"),
            Map.entry("르세라핌", "le_sserafim"),
            Map.entry("le sserafim", "le_sserafim"),
            Map.entry("lesserafim", "le_sserafim"),
            Map.entry("라이즈", "riize"),
            Map.entry("riize", "riize"),
            Map.entry("엔시티", "nct"),
            Map.entry("nct", "nct"),
            Map.entry("nct 127", "nct"),
            Map.entry("엔하이픈", "enhypen"),
            Map.entry("enhypen", "enhypen"),
            Map.entry("캣츠아이", "katseye"),
            Map.entry("katseye", "katseye"),
            Map.entry("빅뱅", "bigbang"),
            Map.entry("bigbang", "bigbang"),
            Map.entry("투모로우바이투게더", "txt"),
            Map.entry("txt", "txt"),
            Map.entry("블랙핑크", "blackpink"),
            Map.entry("blackpink", "blackpink"),
            Map.entry("black pink", "blackpink"),
            Map.entry("제로베이스원", "zerobaseone"),
            Map.entry("zerobaseone", "zerobaseone"),
            Map.entry("보이넥스트도어", "boynextdoor"),
            Map.entry("boynextdoor", "boynextdoor")
    );

    /** 불용어 및 장르 키워드 (단어 유사도 계산 시 장르명 제외) */
    private static final Set<String> STOP_WORDS = Set.of(
            "기자", "종합", "속보", "단독", "오늘", "내일", "어제", "지난", "이번", "연합뉴스",
            "korea", "news", "herald", "times", "yna", "사진", "포토", "영상",
            "kpop", "k-pop", "케이팝", "kculture", "트렌드"
    );

    /** K-POP 관련성 판단을 위한 정밀 키워드/아티스트 목록 */
    private static final List<String> KPOP_RELATED_TERMS = List.of(
            "k-pop", "kpop", "k pop", "케이팝",
            "idol", "아이돌", "boy group", "girl group", "boy band", "girl band",
            "보이그룹", "걸그룹",
            "bts", "blackpink", "black pink", "newjeans", "new jeans", "aespa", "seventeen", "ive",
            "stray kids", "straykids", "twice", "le sserafim", "lesserafim", "txt", "riize", "nct",
            "enhypen", "bigbang", "zerobaseone", "boynextdoor", "katseye",
            "방탄소년단", "뉴진스", "에스파", "세븐틴", "아이브", "스트레이 키즈", "스트레이키즈", "스키즈",
            "트와이스", "르세라핌", "라이즈", "엔시티", "투모로우바이투게더", "엔하이픈", "빅뱅", "캣츠아이"
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

        // 1. 유효성 검사 및 오래된 기사 제외 (로봇/기념메달 등 비관련 노이즈 기사 제외)
        List<CrawledNewsArticle> validArticles = articles.stream()
                .filter(a -> a != null
                        && a.getUrl() != null && !a.getUrl().isBlank()
                        && a.getTitle() != null && !a.getTitle().isBlank()
                        && a.getBody() != null && !a.getBody().isBlank())
                .filter(a -> a.getPublishedAt() == null || !a.getPublishedAt().isBefore(cutoffDate))
                .filter(a -> !isNonKpopNoiseArticle(a.getTitle()))
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

        // 3. 다계층 중복 제거 (URL + 정규화 제목 + 아티스트 엔티티 + 자카드 어휘 유사도)
        List<ScoredArticle> deduplicated = deduplicateArticles(scoredArticles);

        // 4. 최대 N개 제한 및 최종 기사 리스트 반환
        return deduplicated.stream()
                .limit(MAX_SELECTED_ARTICLES)
                .map(ScoredArticle::getArticle)
                .toList();
    }

    private boolean isNonKpopNoiseArticle(String title) {
        if (title == null) return false;
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.contains("로봇") || lower.contains("피지컬 ai") || lower.contains("로봇파크")
                || lower.contains("기념메달") || lower.contains("기념 메달") || lower.contains("손흥민")
                || lower.contains("페이커") || lower.contains("조수미");
    }

    public List<CrawledNewsArticle> selectRelevantArticlesForKeyword(List<CrawledNewsArticle> articles, String targetKeyword) {
        if (targetKeyword == null || targetKeyword.isBlank()) {
            return Collections.emptyList();
        }
        return selectRelevantArticles(articles, List.of(targetKeyword.trim()));
    }

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

        if (!hasDirectTitleMatch && !hasDirectBodyMatch && !hasRelatedTitleMatch && !hasRelatedBodyMatch) {
            return 0;
        }

        int score = 0;
        if (hasDirectTitleMatch) score += TITLE_EXACT_MATCH_SCORE;
        if (hasDirectBodyMatch) score += BODY_EXACT_MATCH_SCORE;
        if (hasRelatedTitleMatch) score += TITLE_RELATED_TERM_SCORE;
        if (hasRelatedBodyMatch) score += BODY_RELATED_TERM_SCORE;

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

    public String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\[[^\\]]*\\]|\\([^\\)]*\\)", " ");
        normalized = normalized.replaceAll("k[\\s-_]*pop", "kpop");
        normalized = normalized.replaceAll("k[\\s-_]*beauty", "kbeauty");
        normalized = normalized.replaceAll("k[\\s-_]*fashion", "kfashion");
        normalized = normalized.replaceAll("[^a-z0-9가-힣\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /**
     * 다계층 중복 제거:
     * 1) URL 일치 검사
     * 2) 정규화 제목 일치 검사
     * 3) 대표 아티스트(Entity) 중복 검사 (동일 아티스트 뉴스는 하루에 최고 점수 1건만 선별)
     * 4) 자카드 어휘 유사도 검사 (동일 사건 중복 배제)
     */
    private List<ScoredArticle> deduplicateArticles(List<ScoredArticle> scoredArticles) {
        List<ScoredArticle> sorted = scoredArticles.stream()
                .sorted(Comparator
                        .comparingInt(ScoredArticle::getScore).reversed()
                        .thenComparing(ScoredArticle::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ScoredArticle> deduplicated = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();
        Set<String> seenEntities = new HashSet<>();
        List<Set<String>> seenWordSets = new ArrayList<>();

        for (ScoredArticle item : sorted) {
            String url = item.getArticle().getUrl() != null ? item.getArticle().getUrl().trim() : "";
            String rawTitle = item.getArticle().getTitle() != null ? item.getArticle().getTitle() : "";
            String normalizedTitle = normalizeTitle(rawTitle);

            // 1. URL 중복
            if (!url.isEmpty() && seenUrls.contains(url)) {
                log.debug("URL 중복으로 기사 제외: url={}", url);
                continue;
            }

            // 2. 제목 단순 중복
            if (!normalizedTitle.isEmpty() && seenTitles.contains(normalizedTitle)) {
                log.debug("정규화 제목 중복으로 기사 제외: title='{}'", rawTitle);
                continue;
            }

            // 3. 아티스트 엔티티 중복 (예: 이미 스트레이키즈 기사가 선별되었다면 다음 스트레이키즈 기사는 건너뜀)
            Set<String> detectedEntities = extractCanonicalEntities(rawTitle);
            if (!detectedEntities.isEmpty()) {
                boolean entityOverlap = false;
                for (String entity : detectedEntities) {
                    if (seenEntities.contains(entity)) {
                        entityOverlap = true;
                        log.info("동일 아티스트/주제 중복으로 기사 제외 (기존: {}, 현재 기사: '{}')", entity, rawTitle);
                        break;
                    }
                }
                if (entityOverlap) {
                    continue;
                }
            } else {
                // 4. 엔티티 미지정 기사의 경우 자카드 어휘 유사도 검사 (핵심 단어 3개 이상 겹치고 유사도 60% 이상 시 중복 배제)
                Set<String> currentWords = extractMeaningfulWords(rawTitle);
                if (currentWords.size() >= 3) {
                    boolean semanticOverlap = false;
                    for (Set<String> seenWords : seenWordSets) {
                        Set<String> intersection = new HashSet<>(currentWords);
                        intersection.retainAll(seenWords);
                        double similarity = calculateJaccardSimilarity(currentWords, seenWords);
                        if (intersection.size() >= 3 && similarity >= SIMILARITY_THRESHOLD) {
                            semanticOverlap = true;
                            log.info("어휘 유사도({}%) 중복으로 기사 제외: '{}'", Math.round(similarity * 100), rawTitle);
                            break;
                        }
                    }
                    if (semanticOverlap) {
                        continue;
                    }
                }
            }

            // 중복 검사를 모두 통과한 고유 기사 등록
            if (!url.isEmpty()) seenUrls.add(url);
            if (!normalizedTitle.isEmpty()) seenTitles.add(normalizedTitle);
            seenEntities.addAll(detectedEntities);
            Set<String> words = extractMeaningfulWords(rawTitle);
            if (!words.isEmpty()) seenWordSets.add(words);

            deduplicated.add(item);
        }

        return deduplicated;
    }

    /**
     * 제목에서 정규화된 대표 아티스트/엔티티를 추출합니다.
     */
    public Set<String> extractCanonicalEntities(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> entities = new HashSet<>();

        for (Map.Entry<String, String> entry : ARTIST_CANONICAL_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                entities.add(entry.getValue());
            }
        }
        return entities;
    }

    /**
     * 텍스트에서 의미 있는 핵심 단어 집합을 추출합니다. (불용어 및 장르 키워드 제외)
     */
    public Set<String> extractMeaningfulWords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\[[^\\]]*\\]|\\([^\\)]*\\)", " ")
                .replaceAll("[^a-z0-9가-힣\\s]", " ");

        String[] tokens = cleaned.split("\\s+");
        Set<String> words = new HashSet<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                words.add(trimmed);
            }
        }
        return words;
    }

    /**
     * 두 단어 집합 간의 자카드 유사도를 계산합니다.
     */
    public double calculateJaccardSimilarity(Set<String> words1, Set<String> words2) {
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size();
    }

    private List<String> getRelatedTerms(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }
        String lower = keyword.trim().toLowerCase(Locale.ROOT);
        return RELATED_TERMS_MAP.getOrDefault(lower, Collections.emptyList());
    }

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
