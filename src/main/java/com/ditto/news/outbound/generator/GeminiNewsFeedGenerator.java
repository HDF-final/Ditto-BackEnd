package com.ditto.news.outbound.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ditto.news.application.port.out.AiNewsFeedGenerator;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.outbound.generator.dto.GeminiNewsFeedPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini 기반 AI 뉴스피드 생성기 아웃바운드 어댑터.
 * 프론트엔드 UI 디자인에 맞춘 세련된 매거진 아티클 형식(헤드라인, 3줄 요약, 본문 서식, 인용구, 콘텐츠 기반 동적 태그)을 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiNewsFeedGenerator implements AiNewsFeedGenerator {

    private final GeminiNewsApiClient geminiClient;

    /** K-POP 대표 아티스트 / 그룹 태그 매핑 */
    private static final Map<String, String> ARTIST_TAG_MAP = Map.ofEntries(
            Map.entry("스트레이 키즈", "#스트레이키즈"),
            Map.entry("스트레이키즈", "#스트레이키즈"),
            Map.entry("스키즈", "#스트레이키즈"),
            Map.entry("stray kids", "#스트레이키즈"),
            Map.entry("straykids", "#스트레이키즈"),
            Map.entry("방탄소년단", "#방탄소년단"),
            Map.entry("bts", "#BTS"),
            Map.entry("뉴진스", "#뉴진스"),
            Map.entry("newjeans", "#뉴진스"),
            Map.entry("new jeans", "#뉴진스"),
            Map.entry("에스파", "#에스파"),
            Map.entry("aespa", "#에스파"),
            Map.entry("세븐틴", "#세븐틴"),
            Map.entry("seventeen", "#세븐틴"),
            Map.entry("아이브", "#아이브"),
            Map.entry("ive", "#아이브"),
            Map.entry("트와이스", "#트와이스"),
            Map.entry("twice", "#트와이스"),
            Map.entry("르세라핌", "#르세라핌"),
            Map.entry("le sserafim", "#르세라핌"),
            Map.entry("lesserafim", "#르세라핌"),
            Map.entry("라이즈", "#라이즈"),
            Map.entry("riize", "#라이즈"),
            Map.entry("엔시티 127", "#NCT127"),
            Map.entry("nct 127", "#NCT127"),
            Map.entry("엔시티", "#NCT"),
            Map.entry("nct", "#NCT"),
            Map.entry("엔하이픈", "#엔하이픈"),
            Map.entry("enhypen", "#엔하이픈"),
            Map.entry("빅뱅", "#빅뱅"),
            Map.entry("bigbang", "#빅뱅"),
            Map.entry("투모로우바이투게더", "#투모로우바이투게더"),
            Map.entry("txt", "#TXT"),
            Map.entry("블랙핑크", "#블랙핑크"),
            Map.entry("blackpink", "#블랙핑크"),
            Map.entry("제로베이스원", "#제로베이스원"),
            Map.entry("zerobaseone", "#제로베이스원"),
            Map.entry("보이넥스트도어", "#보이넥스트도어"),
            Map.entry("boynextdoor", "#보이넥스트도어"),
            Map.entry("아이오아이", "#아이오아이"),
            Map.entry("데이식스", "#데이식스"),
            Map.entry("day6", "#DAY6"),
            Map.entry("플레이브", "#플레이브"),
            Map.entry("plave", "#플레이브"),
            Map.entry("아일릿", "#아일릿"),
            Map.entry("illit", "#아일릿"),
            Map.entry("베이비몬스터", "#베이비몬스터"),
            Map.entry("babymonster", "#베이비몬스터"),
            Map.entry("보이그룹", "#보이그룹"),
            Map.entry("걸그룹", "#걸그룹")
    );

    /** 핵심 이벤트 / 트렌드 태그 매핑 */
    private static final Map<String, String> EVENT_TAG_MAP = Map.ofEntries(
            Map.entry("빌보드", "#빌보드1위"),
            Map.entry("billboard", "#빌보드"),
            Map.entry("월드투어", "#월드투어"),
            Map.entry("world tour", "#월드투어"),
            Map.entry("tour", "#월드투어"),
            Map.entry("투어", "#월드투어"),
            Map.entry("컴백", "#컴백대전"),
            Map.entry("comeback", "#컴백대전"),
            Map.entry("콘서트", "#콘서트"),
            Map.entry("concert", "#콘서트"),
            Map.entry("팝업", "#팝업스토어"),
            Map.entry("popup", "#팝업스토어"),
            Map.entry("신보", "#신보발매"),
            Map.entry("신곡", "#신곡발매"),
            Map.entry("album", "#신보발매"),
            Map.entry("음원", "#음원차트"),
            Map.entry("쇼케이스", "#쇼케이스"),
            Map.entry("팬덤", "#글로벌팬덤"),
            Map.entry("수출", "#K컬처수출"),
            Map.entry("뷰티", "#K뷰티"),
            Map.entry("패션", "#K패션"),
            Map.entry("더현대", "#더현대서울"),
            Map.entry("성수", "#성수핫플")
    );

    @Override
    public GeneratedNewsFeed generate(List<CrawledNewsArticle> articles, String topicKeyword) {
        if (articles == null || articles.isEmpty()) {
            log.warn("뉴스피드를 생성할 기사 목록이 비어있습니다. topic={}", topicKeyword);
            return null;
        }

        String topic = topicKeyword != null && !topicKeyword.isBlank() ? topicKeyword.trim() : "K-POP";
        String representativeImageUrl = extractRepresentativeImageUrl(articles);
        String slug = generateSlug(topic);
        String sourceAttribution = buildSourceAttribution(articles);

        // 1. Gemini LLM 구조화 생성 시도
        GeminiNewsFeedPayload payload = geminiClient.generateRewrittenNews(articles, topic);
        if (payload != null && payload.getTitle() != null && !payload.getTitle().isBlank()) {
            String title = cleanHeadline(payload.getTitle());
            List<String> summaries = payload.getSummaries() != null && !payload.getSummaries().isEmpty()
                    ? payload.getSummaries()
                    : buildEditorialSummaries(articles, topic);

            StringBuilder bodyBuilder = new StringBuilder();
            if (payload.getBody() != null && !payload.getBody().isBlank()) {
                bodyBuilder.append(payload.getBody().trim());
            }
            if (!sourceAttribution.isBlank()) {
                bodyBuilder.append("\n\n").append(sourceAttribution);
            }

            List<String> keywords = buildDynamicKeywords(articles, topic, payload.getKeywords());

            return GeneratedNewsFeed.builder()
                    .title(title)
                    .summaries(summaries)
                    .body(bodyBuilder.toString().trim())
                    .slug(slug)
                    .representativeImageUrl(representativeImageUrl)
                    .keywords(keywords)
                    .build();
        }

        // 2. Gemini 미사용/실패 시 매거진 템플릿 기반 안전 폴백 생성
        log.info("Gemini 미사용/호출실패로 고품질 매거진 템플릿 피드 생성을 수행합니다. topic={}", topic);
        return generateMagazineFallback(articles, topic, representativeImageUrl, slug, sourceAttribution);
    }

    private GeneratedNewsFeed generateMagazineFallback(
            List<CrawledNewsArticle> articles,
            String topic,
            String representativeImageUrl,
            String slug,
            String sourceAttribution) {

        CrawledNewsArticle mainArticle = articles.get(0);
        String title = cleanHeadline(mainArticle.getTitle());
        List<String> summaries = buildEditorialSummaries(articles, topic);

        StringBuilder bodyBuilder = new StringBuilder();

        // 1문단: 배경 및 도입
        bodyBuilder.append(String.format("글로벌 %s 트렌드가 빠르게 확산되며 팬덤 중심의 소비와 새로운 콘텐츠 경험이 국내외 시장의 흐름을 바꾸고 있습니다.\n\n", topic));

        // 2문단: 세부 사실 및 현장 이야기
        if (mainArticle.getBody() != null && !mainArticle.getBody().isBlank()) {
            String clean = mainArticle.getBody().replaceAll("\\s+", " ").trim();
            String snippet = clean.length() > 250 ? clean.substring(0, 250) + "..." : clean;
            bodyBuilder.append(snippet).append("\n\n");
        }

        // 3문단: 인용구 블록 (UI 디자인 매거진 스타일)
        bodyBuilder.append(String.format("“현장에서 체감하는 %s 문화 경험이 귀국 후 소비와 글로벌 팬덤으로 이어지는 핵심 동력입니다.”\n- DITTO Trend Lab\n\n", topic));

        // 4문단: DITTO 코스 연계 및 마무리 제언
        bodyBuilder.append(String.format("DITTO는 앞으로도 %s 콘텐츠와 실제 여행 경험이 만나는 스팟을 지속적으로 추적합니다. 사용자가 저장한 코스와 뉴스 관심사를 연결해 실제 방문 가능한 추천 스팟으로 확장할 예정입니다.", topic));

        if (!sourceAttribution.isBlank()) {
            bodyBuilder.append("\n\n").append(sourceAttribution);
        }

        List<String> keywords = buildDynamicKeywords(articles, topic, Collections.emptyList());

        return GeneratedNewsFeed.builder()
                .title(title)
                .summaries(summaries)
                .body(bodyBuilder.toString().trim())
                .slug(slug)
                .representativeImageUrl(representativeImageUrl)
                .keywords(keywords)
                .build();
    }

    private List<String> buildEditorialSummaries(List<CrawledNewsArticle> articles, String topic) {
        List<String> summaries = new ArrayList<>();
        for (CrawledNewsArticle a : articles) {
            String cleaned = cleanHeadline(a.getTitle());
            if (!cleaned.isBlank()) {
                summaries.add(cleaned);
            }
            if (summaries.size() >= 3) break;
        }
        while (summaries.size() < 3) {
            if (summaries.isEmpty()) summaries.add(topic + " 글로벌 트렌드 및 시장 확장세 지속");
            else if (summaries.size() == 1) summaries.add("팬덤 및 현장 경험 중심의 새로운 소비 흐름 형성");
            else summaries.add("여행과 K-컬처가 만나는 주요 브랜드 스팟 주목");
        }
        return summaries;
    }

    /**
     * 기사 본문과 제목에서 등장하는 실제 아이돌/아티스트명과 핵심 사건 키워드를 동적으로 추출하여 고유 해시태그를 생성합니다.
     */
    public List<String> buildDynamicKeywords(List<CrawledNewsArticle> articles, String topic, List<String> aiKeywords) {
        Set<String> resultSet = new LinkedHashSet<>();

        // 1. AI(Gemini)가 생성한 키워드가 있으면 정제하여 우선 반영
        if (aiKeywords != null && !aiKeywords.isEmpty()) {
            for (String kw : aiKeywords) {
                if (kw == null || kw.isBlank()) continue;
                String formatted = kw.trim();
                if (!formatted.startsWith("#")) {
                    formatted = "#" + formatted;
                }
                resultSet.add(formatted);
                if (resultSet.size() >= 4) break;
            }
        }

        // 2. 기사 본문 및 제목 스캔을 통해 실제 아티스트명/그룹명 태그 추출
        StringBuilder fullTextBuilder = new StringBuilder();
        if (articles != null) {
            for (CrawledNewsArticle article : articles) {
                if (article.getTitle() != null) fullTextBuilder.append(" ").append(article.getTitle());
                if (article.getBody() != null) fullTextBuilder.append(" ").append(article.getBody());
            }
        }
        String fullTextLower = fullTextBuilder.toString().toLowerCase(Locale.ROOT);

        // 아티스트 태그 탐색
        for (Map.Entry<String, String> entry : ARTIST_TAG_MAP.entrySet()) {
            if (fullTextLower.contains(entry.getKey())) {
                resultSet.add(entry.getValue());
                if (resultSet.size() >= 4) break;
            }
        }

        // 핵심 사건/트렌드 태그 탐색
        for (Map.Entry<String, String> entry : EVENT_TAG_MAP.entrySet()) {
            if (fullTextLower.contains(entry.getKey())) {
                resultSet.add(entry.getValue());
                if (resultSet.size() >= 4) break;
            }
        }

        // 3. 토픽 기본 태그 보충 (최소 3개 유지)
        String topicTag = "#" + topic.replaceAll("[^a-zA-Z0-9가-힣]", "");
        if (!topicTag.equals("#")) {
            resultSet.add(topicTag);
        }
        resultSet.add("#KCulture");
        resultSet.add("#트렌드");

        return resultSet.stream().limit(4).toList();
    }

    private String cleanHeadline(String rawTitle) {
        if (rawTitle == null) return "";
        return rawTitle.replaceAll("\\[.*?\\]", "")
                .replaceAll("\\|.*$", "")
                .replaceAll(" - .*$", "")
                .replaceAll("(?i)yna|연합뉴스|korea herald|korea times", "")
                .replaceAll("^[\\s:·,]+", "")
                .replaceAll("[\\s:·,]+$", "")
                .trim();
    }

    private String extractRepresentativeImageUrl(List<CrawledNewsArticle> articles) {
        for (CrawledNewsArticle article : articles) {
            if (article.getImageUrl() != null && !article.getImageUrl().isBlank()) {
                return article.getImageUrl();
            }
        }
        return null;
    }

    private String generateSlug(String topic) {
        return topic.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildSourceAttribution(List<CrawledNewsArticle> articles) {
        Set<String> sources = new LinkedHashSet<>();
        for (CrawledNewsArticle a : articles) {
            if (a.getSource() != null && !a.getSource().isBlank()) {
                sources.add(a.getSource().trim());
            }
        }
        if (sources.isEmpty()) {
            return "";
        }
        return "출처: " + String.join(", ", sources);
    }
}
