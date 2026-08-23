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
 * 프론트엔드 UI 디자인에 맞춘 세련된 매거진 아티클 형식(헤드라인, 3줄 요약, 본문 서식, 맞춤형 DITTO Trend Lab 인용구, 동적 태그, 출처 링크)을 생성합니다.
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
        String rawTitle = mainArticle.getTitle() != null ? mainArticle.getTitle() : "";
        String rawBody = mainArticle.getBody() != null ? mainArticle.getBody() : "";
        String title = cleanHeadline(rawTitle);
        List<String> summaries = buildEditorialSummaries(articles, topic);

        StringBuilder bodyBuilder = new StringBuilder();

        // 1문단: 배경 및 도입
        bodyBuilder.append(String.format("글로벌 %s 트렌드가 빠르게 확산되며 팬덤 중심의 소비와 새로운 콘텐츠 경험이 국내외 시장의 흐름을 바꾸고 있습니다.\n\n", topic));

        // 2문단: 세부 사실 및 현장 이야기 (기자 이메일/사진 캡션 등 노이즈 정제 및 전체 본문 보존)
        if (!rawBody.isBlank()) {
            String clean = cleanArticleBody(rawBody);
            if (!clean.isBlank()) {
                bodyBuilder.append(clean).append("\n\n");
            }
        }

        // 3문단: 기사 내용 맞춤형 DITTO Trend Lab 핵심 인사이트 인용구 블록
        String insightQuote = buildDynamicInsightQuote(rawTitle, rawBody, topic);
        bodyBuilder.append(insightQuote);

        // 4문단: 기사 주인공/사건 맞춤형 DITTO 코스 연계 및 마무리 제언
        String conclusion = buildDynamicConclusion(rawTitle, topic);
        bodyBuilder.append(conclusion);

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

    /**
     * 기사의 주인공 아티스트와 핵심 사건을 분석하여 맞춤형 DITTO Trend Lab 분석 인용구를 생성합니다.
     */
    public String buildDynamicInsightQuote(String title, String body, String topic) {
        String full = (title + " " + body).toLowerCase(Locale.ROOT);

        if (full.contains("스트레이 키즈") || full.contains("스트레이키즈") || full.contains("stray kids") || full.contains("스키즈")) {
            if (full.contains("빌보드") || full.contains("billboard") || full.contains("1위")) {
                return "“스트레이 키즈의 글로벌 빌보드 9연속 1위는 단순 음원 성적을 넘어, K-POP 아티스트의 강력한 IP가 오프라인 성지순례와 글로벌 투어 소비로 확장되는 결정적 전환점입니다.”\n- DITTO Trend Lab\n\n";
            }
            return "“스트레이 키즈의 폭발적인 글로벌 성장은 전 세계 팬덤을 한국 현장 체험과 오프라인 명소로 이끄는 핵심 동력입니다.”\n- DITTO Trend Lab\n\n";
        }
        if (full.contains("빅뱅") || full.contains("엔하이픈") || full.contains("nct") || full.contains("보이그룹")) {
            if (full.contains("컴백")) {
                return "“8월 대형 보이그룹들의 연이은 컴백 대전은 팬덤 중심의 앨범 팝업스토어 및 현장 체험 수요를 단기간에 집중시키는 기폭제가 되고 있습니다.”\n- DITTO Trend Lab\n\n";
            }
            return "“차세대 보이그룹들의 활발한 글로벌 활동은 한국 대중문화 현장의 생생한 열기를 전 세계 팬들에게 직접 전달하는 가교 역할을 합니다.”\n- DITTO Trend Lab\n\n";
        }
        if (full.contains("뉴진스") || full.contains("newjeans") || full.contains("new jeans")) {
            return "“뉴진스의 독창적인 음악적 시도와 감각적인 비주얼은 글로벌 여행자들에게 가장 트렌디한 서울의 감성과 라이프스타일을 각인시키고 있습니다.”\n- DITTO Trend Lab\n\n";
        }
        if (full.contains("에스파") || full.contains("aespa")) {
            return "“에스파의 독보적인 세계관과 글로벌 성과는 K-POP과 미래형 콘텐츠 경험이 결합된 새로운 현장 문화 소비를 창출하고 있습니다.”\n- DITTO Trend Lab\n\n";
        }
        if (full.contains("팝업") || full.contains("더현대") || full.contains("성수")) {
            return "“도심 곳곳에서 펼쳐지는 팝업스토어와 트렌드 스팟은 글로벌 여행자가 한국의 최신 라이프스타일을 가장 직관적으로 체험할 수 있는 최적의 무대입니다.”\n- DITTO Trend Lab\n\n";
        }
        if (full.contains("뷰티") || full.contains("화장품")) {
            return "“K-뷰티 인디 브랜드의 글로벌 약진은 단순한 제품 소비를 넘어 한국 뷰티 살롱과 피부 케어 투어로 이어지는 고부가가치 여행 트렌드를 형성하고 있습니다.”\n- DITTO Trend Lab\n\n";
        }

        String cleanTitle = cleanHeadline(title);
        return String.format("“%s 관련 성과는 글로벌 팬덤이 실제 한국 여행 및 현장 문화 소비로 이어지는 중요한 트렌드 지표입니다.”\n- DITTO Trend Lab\n\n", cleanTitle);
    }

    /**
     * 기사 주인공/사건에 맞춘 마무리 제언 문구를 생성합니다.
     */
    public String buildDynamicConclusion(String title, String topic) {
        String lower = (title != null ? title : "").toLowerCase(Locale.ROOT);
        if (lower.contains("스트레이 키즈") || lower.contains("스트레이키즈") || lower.contains("stray kids") || lower.contains("스키즈")) {
            return "DITTO는 스트레이 키즈의 글로벌 행보와 함께, 팬들이 직접 방문할 수 있는 소속사 인근 성지 및 추천 명소를 맞춤형 코스로 연결해 제공합니다.";
        }
        if (lower.contains("빅뱅") || lower.contains("엔하이픈") || lower.contains("nct")) {
            return "DITTO는 8월 컴백 아티스트들의 공식 팝업스토어 및 서울 주요 핫플레이스를 연계한 특별 큐레이션 코스를 지속적으로 업데이트합니다.";
        }
        if (lower.contains("뉴진스") || lower.contains("newjeans")) {
            return "DITTO는 뉴진스의 뮤직비디오 촬영지와 앨범 팝업 공간을 둘러볼 수 있는 감성 여행 코스를 추천합니다.";
        }
        if (lower.contains("팝업") || lower.contains("성수") || lower.contains("더현대")) {
            return "DITTO는 매주 새롭게 열리는 핫한 팝업스토어 일정과 주변 맛집·카페를 한눈에 둘러볼 수 있는 맞춤형 탐방 코스를 지원합니다.";
        }
        return String.format("DITTO는 앞으로도 %s 최신 트렌드와 사용자의 관심사를 분석하여, 실제 방문 가능한 매력적인 오프라인 추천 코스로 확장할 예정입니다.", topic);
    }

    /**
     * 원문 기사 본문에서 기자명, 이메일, 사진 캡션 등의 노이즈를 깔끔하게 정제합니다.
     */
    public String cleanArticleBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }
        return rawBody
                // 기자 이메일 제거 (예: ryousanta@yna.co.kr)
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "")
                // (서울=연합뉴스) OOO 기자 = ... 패턴 제거
                .replaceAll("\\([가-힣\\s]+=[가-힣\\s]+\\)[^=]*?기자\\s*=", "")
                // = 사진 ... 제거
                .replaceAll("=\\s*사진[^\\n]*", "")
                // 날짜 패턴 정리
                .replaceAll("\\d{4}\\.\\s*\\d{1,2}\\.\\s*\\d{1,2}", "")
                // [사진], [포토] 대괄호 캡션 제거
                .replaceAll("\\[[^\\]]*사진[^\\]]*\\]|\\[[^\\]]*포토[^\\]]*\\]", "")
                .replaceAll("(?i)\\[[^\\]]*photo[^\\]]*\\]", "")
                // 연속 공백 및 줄바꿈 정리
                .replaceAll("[ \\t]+", " ")
                .replaceAll("(\\r?\\n\\s*){2,}", "\n\n")
                .trim();
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

    /**
     * 출처 이름과 원문 기사 링크 URL을 클릭 가능한 마크다운 링크로 조합합니다.
     * 예: "출처: [연합뉴스](https://www.yna.co.kr/view/AKR2026...)"
     */
    private String buildSourceAttribution(List<CrawledNewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return "";
        }
        CrawledNewsArticle main = articles.get(0);
        String sourceName = (main.getSource() != null && !main.getSource().isBlank())
                ? main.getSource().trim()
                : "연합뉴스";
        String sourceUrl = (main.getUrl() != null && !main.getUrl().isBlank())
                ? main.getUrl().trim()
                : "";

        if (!sourceUrl.isEmpty()) {
            return String.format("출처: [%s](%s)", sourceName, sourceUrl);
        }
        return "출처: " + sourceName;
    }
}
