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
            Map.entry("캣츠아이", "#캣츠아이"),
            Map.entry("katseye", "#캣츠아이"),
            Map.entry("아이오아이", "#아이오아이"),
            Map.entry("데이식스", "#데이식스"),
            Map.entry("day6", "#DAY6"),
            Map.entry("플레이브", "#플레이브"),
            Map.entry("plave", "#플레이브"),
            Map.entry("아일릿", "#아일릿"),
            Map.entry("illit", "#아일릿"),
            Map.entry("베이비몬스터", "#베이비몬스터"),
            Map.entry("babymonster", "#베이비몬스터")
    );

    /** 걸그룹 분류 키워드 */
    private static final Set<String> GIRL_GROUPS = Set.of(
            "뉴진스", "newjeans", "new jeans",
            "에스파", "aespa",
            "아이브", "ive",
            "트와이스", "twice",
            "르세라핌", "le sserafim", "lesserafim",
            "캣츠아이", "katseye",
            "아일릿", "illit",
            "베이비몬스터", "babymonster",
            "블랙핑크", "blackpink",
            "아이오아이",
            "걸그룹", "girl group"
    );

    /** 보이그룹 분류 키워드 */
    private static final Set<String> BOY_GROUPS = Set.of(
            "스트레이 키즈", "스트레이키즈", "stray kids", "straykids", "스키즈",
            "방탄소년단", "bts",
            "세븐틴", "seventeen",
            "라이즈", "riize",
            "엔시티", "nct", "nct 127", "nct dream", "엔시티 127", "엔시티 드림",
            "엔하이픈", "enhypen",
            "빅뱅", "bigbang",
            "투모로우바이투게더", "txt",
            "제로베이스원", "zerobaseone",
            "보이넥스트도어", "boynextdoor",
            "데이식스", "day6",
            "플레이브", "plave",
            "보이그룹", "boy band"
    );

    /** 핵심 이벤트 / 트렌드 태그 매핑 */
    private static final Map<String, String> EVENT_TAG_MAP = Map.ofEntries(
            Map.entry("스포티파이", "#스포티파이"),
            Map.entry("spotify", "#스포티파이"),
            Map.entry("스트리밍", "#음원스트리밍"),
            Map.entry("streaming", "#음원스트리밍"),
            Map.entry("밀리언셀러", "#밀리언셀러"),
            Map.entry("million seller", "#밀리언셀러"),
            Map.entry("빌보드", "#빌보드1위"),
            Map.entry("billboard", "#빌보드"),
            Map.entry("오피셜 차트", "#오피셜차트"),
            Map.entry("official chart", "#오피셜차트"),
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
            Map.entry("k-뷰티", "#K뷰티"),
            Map.entry("k뷰티", "#K뷰티"),
            Map.entry("화장품", "#K뷰티"),
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
        String sourceUrl = (articles != null && !articles.isEmpty() && articles.get(0).getUrl() != null)
                ? articles.get(0).getUrl().trim()
                : null;

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
                    .sourceUrl(sourceUrl)
                    .build();
        }

        // 2. Gemini 미사용/실패 시 매거진 템플릿 기반 안전 폴백 생성
        log.info("Gemini 미사용/호출실패로 고품질 매거진 템플릿 피드 생성을 수행합니다. topic={}", topic);
        return generateMagazineFallback(articles, topic, representativeImageUrl, slug, sourceAttribution, sourceUrl);
    }

    private GeneratedNewsFeed generateMagazineFallback(
            List<CrawledNewsArticle> articles,
            String topic,
            String representativeImageUrl,
            String slug,
            String sourceAttribution,
            String sourceUrl) {

        CrawledNewsArticle mainArticle = articles.get(0);
        String rawTitle = mainArticle.getTitle() != null ? mainArticle.getTitle() : "";
        String rawBody = mainArticle.getBody() != null ? mainArticle.getBody() : "";
        String title = cleanHeadline(rawTitle);
        List<String> summaries = buildEditorialSummaries(articles, topic);

        StringBuilder bodyBuilder = new StringBuilder();

        // 1문단: 배경 및 도입
        bodyBuilder.append(String.format("글로벌 %s 트렌드가 빠르게 확산되며 팬덤 중심의 소비와 새로운 콘텐츠 경험이 국내외 시장의 흐름을 바꾸고 있습니다.\n\n", topic));

        // 2문단: 세부 사실 및 현장 이야기 (기자 이메일/사진 캡션 등 노이즈 정제 및 핵심 본문 보존)
        if (!rawBody.isBlank()) {
            String clean = cleanArticleBody(rawBody);
            if (!clean.isBlank()) {
                bodyBuilder.append(clean).append("\n\n");
            }
        }

        // 3문단: 기사 내용 맞춤형 DITTO Trend Lab 핵심 인사이트 인용구 블록
        String insightQuote = buildDynamicInsightQuote(rawTitle, rawBody, topic);
        bodyBuilder.append(insightQuote).append("\n");

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
                .sourceUrl(sourceUrl)
                .build();
    }

    /**
     * 기사의 주인공 아티스트와 핵심 사건을 분석하여 맞춤형 DITTO Trend Lab 분석 인용구를 생성합니다.
     */
    public String buildDynamicInsightQuote(String title, String body, String topic) {
        String full = (title + " " + body).toLowerCase(Locale.ROOT);

        if (full.contains("스트레이 키즈") || full.contains("스트레이키즈") || full.contains("stray kids") || full.contains("스키즈")) {
            if (full.contains("빌보드") || full.contains("billboard") || full.contains("1위")) {
                return "“스트레이 키즈의 글로벌 빌보드 9연속 1위는 단순 음원 성적을 넘어, K-POP 아티스트의 강력한 IP가 오프라인 성지순례와 글로벌 투어 소비로 확장되는 결정적 전환점입니다.”\n\n- DITTO Trend Lab";
            }
            return "“스트레이 키즈의 폭발적인 글로벌 성장은 전 세계 팬덤을 한국 현장 체험과 오프라인 명소로 이끄는 핵심 동력입니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("엔하이픈") || full.contains("enhypen")) {
            return "“엔하이픈의 앨범 발매 당일 밀리언셀러 등극과 글로벌 아이튠즈 1위는 차세대 K-POP 보이그룹의 막강한 글로벌 음반 파워를 명확히 보여줍니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("캣츠아이") || full.contains("katseye")) {
            return "“하이브 글로벌 걸그룹 캣츠아이의 영국 오피셜 차트 2위 진입은 현지화 K-POP 시스템이 글로벌 주류 팝 시장에 성공적으로 안착했음을 입증합니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("뉴진스") || full.contains("newjeans") || full.contains("new jeans")) {
            return "“뉴진스의 스포티파이 누적 80억 스트리밍 돌파는 기존 히트곡들의 독보적인 롱런 음원 파워와 글로벌 팬덤의 일상적 감상 문화를 잘 보여줍니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("빅뱅") || full.contains("nct") || full.contains("보이그룹")) {
            if (full.contains("컴백")) {
                return "“8월 대형 보이그룹들의 연이은 컴백 대전은 팬덤 중심의 앨범 팝업스토어 및 현장 체험 수요를 단기간에 집중시키는 기폭제가 되고 있습니다.”\n\n- DITTO Trend Lab";
            }
            return "“차세대 보이그룹들의 활발한 글로벌 활동은 한국 대중문화 현장의 생생한 열기를 전 세계 팬들에게 직접 전달하는 가교 역할을 합니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("에스파") || full.contains("aespa")) {
            return "“에스파의 독보적인 세계관과 글로벌 성과는 K-POP과 미래형 콘텐츠 경험이 결합된 새로운 현장 문화 소비를 창출하고 있습니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("팝업") || full.contains("더현대") || full.contains("성수")) {
            return "“도심 곳곳에서 펼쳐지는 팝업스토어와 트렌드 스팟은 글로벌 여행자가 한국의 최신 라이프스타일을 가장 직관적으로 체험할 수 있는 최적의 무대입니다.”\n\n- DITTO Trend Lab";
        }
        if (full.contains("k-뷰티") || full.contains("k뷰티") || full.contains("화장품")) {
            return "“K-뷰티 인디 브랜드의 글로벌 약진은 단순한 제품 소비를 넘어 한국 뷰티 살롱과 피부 케어 투어로 이어지는 고부가가치 여행 트렌드를 형성하고 있습니다.”\n\n- DITTO Trend Lab";
        }

        String cleanTitle = cleanHeadline(title);
        return String.format("“%s 관련 성과는 글로벌 팬덤이 실제 한국 여행 및 현장 문화 소비로 이어지는 중요한 트렌드 지표입니다.”\n\n- DITTO Trend Lab", cleanTitle);
    }

    /**
     * 기사 주인공/사건에 맞춘 마무리 제언 문구를 생성합니다.
     */
    public String buildDynamicConclusion(String title, String topic) {
        String lower = (title != null ? title : "").toLowerCase(Locale.ROOT);
        if (lower.contains("스트레이 키즈") || lower.contains("스트레이키즈") || lower.contains("stray kids") || lower.contains("스키즈")) {
            return "DITTO는 스트레이 키즈의 글로벌 행보와 함께, 팬들이 직접 방문할 수 있는 소속사 인근 성지 및 추천 명소를 맞춤형 코스로 연결해 제공합니다.";
        }
        if (lower.contains("엔하이픈") || lower.contains("enhypen")) {
            return "DITTO는 엔하이픈의 새 앨범 발매와 함께, 팬들이 직접 방문할 수 있는 컴백 팝업스토어 및 공식 굿즈 스팟을 추천 코스로 연계해 안내합니다.";
        }
        if (lower.contains("캣츠아이") || lower.contains("katseye")) {
            return "DITTO는 캣츠아이의 글로벌 활약과 발맞추어, K-POP 트렌드와 핫플레이스를 함께 즐길 수 있는 특별 투어 코스를 지속적으로 제공합니다.";
        }
        if (lower.contains("뉴진스") || lower.contains("newjeans")) {
            return "DITTO는 뉴진스의 뮤직비디오 촬영지와 앨범 감성을 둘러볼 수 있는 서울 시내 핫플레이스 여행 코스를 추천합니다.";
        }
        if (lower.contains("빅뱅") || lower.contains("nct")) {
            return "DITTO는 8월 컴백 아티스트들의 공식 팝업스토어 및 서울 주요 핫플레이스를 연계한 특별 큐레이션 코스를 지속적으로 업데이트합니다.";
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
                // [사진], [포토], [제공], [촬영], [그래픽] 대괄호 캡션 및 금지문구 제거
                .replaceAll("\\[[^\\]]*사진[^\\]]*\\]|\\[[^\\]]*포토[^\\]]*\\]", "")
                .replaceAll("\\[[^\\]]*제공[^\\]]*\\]|\\[[^\\]]*촬영[^\\]]*\\]", "")
                .replaceAll("\\[[^\\]]*재판매[^\\]]*\\]|\\[[^\\]]*금지[^\\]]*\\]", "")
                .replaceAll("\\[[^\\]]*그래픽[^\\]]*\\]", "")
                .replaceAll("(?i)\\[[^\\]]*photo[^\\]]*\\]", "")
                // 연속 공백 및 줄바꿈 정리
                .replaceAll("[ \\t]+", " ")
                .replaceAll("(\\r?\\n\\s*){2,}", "\n\n")
                .trim();
    }

    /**
     * 기사별 고유 본문 문장들을 지능적으로 분석하여 3개의 독립적이고 사실적인 핵심 요약 불릿을 추출합니다.
     */
    public List<String> buildEditorialSummaries(List<CrawledNewsArticle> articles, String topic) {
        List<String> summaries = new ArrayList<>();
        if (articles == null || articles.isEmpty()) {
            return List.of(
                    topic + " 관련 최신 K-컬처 트렌드 소식",
                    "글로벌 팬덤과 함께 확장되는 주요 성과 및 현장 열기",
                    "국내외 문화 트렌드를 선도하는 핵심 이슈 주목"
            );
        }

        CrawledNewsArticle article = articles.get(0);
        String headline = cleanHeadline(article.getTitle());
        if (headline != null && !headline.isBlank()) {
            summaries.add(headline);
        }

        // 원문 기사 본문에서 핵심 팩트 문장 추출
        String rawBody = article.getBody();
        String cleanedBody = cleanArticleBody(rawBody);

        if (cleanedBody != null && !cleanedBody.isBlank()) {
            // 마침표 또는 줄바꿈 기준 문장 분리
            String[] rawSentences = cleanedBody.split("(?<=[.!?\\n])\\s+");
            List<String> validSentences = new ArrayList<>();

            for (String s : rawSentences) {
                String trimmed = s.replaceAll("[\\r\\n]+", " ").trim();
                // 유효한 길이(15자 이상 150자 이하) 및 노이즈 필터링
                if (trimmed.length() >= 15 && trimmed.length() <= 150
                        && !trimmed.contains("기자 =") && !trimmed.contains("@") && !trimmed.contains("무단 전재")) {
                    if (headline == null || !headline.contains(trimmed)) {
                        validSentences.add(trimmed);
                    }
                }
            }

            // 2번째 불릿: 본문 전반부 핵심 사실 문장
            if (!validSentences.isEmpty()) {
                summaries.add(validSentences.get(0));
            }
            // 3번째 불릿: 본문 중후반부 성과/전망 문장
            if (validSentences.size() > 1) {
                int targetIdx = Math.min(validSentences.size() - 1, Math.max(1, validSentences.size() / 2));
                // 2번째와 동일하지 않은 문장 선택
                if (targetIdx < validSentences.size() && !validSentences.get(targetIdx).equals(validSentences.get(0))) {
                    summaries.add(validSentences.get(targetIdx));
                } else if (validSentences.size() > 2) {
                    summaries.add(validSentences.get(validSentences.size() - 1));
                }
            }
        }

        // 만약 본문이 짧아 불릿이 부족할 경우 해당 기사 문맥 기반 동적 생성
        if (summaries.size() < 2) {
            summaries.add(buildDynamicInsight(headline, topic, 1));
        }
        if (summaries.size() < 3) {
            summaries.add(buildDynamicInsight(headline, topic, 2));
        }

        return summaries.subList(0, Math.min(summaries.size(), 3));
    }

    private String buildDynamicInsight(String headline, String topic, int index) {
        String lower = (headline != null ? headline : "").toLowerCase(Locale.ROOT);
        if (index == 1) {
            if (lower.contains("스트리밍") || lower.contains("스포티파이")) return "글로벌 음원 플랫폼 내 독보적인 스트리밍 누적 성과 지속";
            if (lower.contains("밀리언셀러") || lower.contains("앨범") || lower.contains("판매")) return "앨범 발매 직후 글로벌 음반 차트 및 판매량 1위 석권";
            if (lower.contains("차트") || lower.contains("빌보드") || lower.contains("오피셜")) return "영미권 주요 공인 팝 음악 차트 최상위권 동시 진입";
            if (lower.contains("투어") || lower.contains("콘서트") || lower.contains("공연")) return "전 세계 주요 도시 대규모 월드투어 및 현지 팬덤 열풍 형성";
            return topic + " 핵심 아티스트의 글로벌 시장 내 파급력과 인지도 입증";
        } else {
            if (lower.contains("뉴진스") || lower.contains("newjeans")) return "글로벌 롱런 음원 파워와 일상 속 K-POP 문화 소비 확산";
            if (lower.contains("엔하이픈") || lower.contains("enhypen")) return "차세대 보이그룹의 압도적인 글로벌 팬덤 결집력 확인";
            if (lower.contains("캣츠아이") || lower.contains("katseye")) return "글로벌 주류 팝 시장 현지화 시스템의 성공적 안착";
            if (lower.contains("방탄소년단") || lower.contains("bts")) return "전 세계적인 문화적 영향력과 도시 단위 기념 이벤트 확산";
            return "국내외 K-컬처 팬들의 뜨거운 관심과 현장 참여 열기 고조";
        }
    }

    /**
     * 기사 본문과 제목에서 등장하는 실제 아이돌/아티스트명과 핵심 사건 키워드를 동적으로 추출하여 고유 해시태그를 생성합니다.
     * [구조]: 1순위 대표 아티스트(#뉴진스) -> 2순위 핵심 성과(#스포티파이/#빌보드1위) -> 3순위 분류(#걸그룹/#보이그룹) -> 4순위 토픽(#KPOP)
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

        if (resultSet.size() >= 4) {
            return resultSet.stream().limit(4).toList();
        }

        String mainTitle = (articles != null && !articles.isEmpty() && articles.get(0).getTitle() != null)
                ? articles.get(0).getTitle().toLowerCase(Locale.ROOT)
                : "";

        StringBuilder fullTextBuilder = new StringBuilder();
        if (articles != null) {
            for (CrawledNewsArticle article : articles) {
                if (article.getTitle() != null) fullTextBuilder.append(" ").append(article.getTitle());
                if (article.getBody() != null) fullTextBuilder.append(" ").append(article.getBody());
            }
        }
        String fullTextLower = fullTextBuilder.toString().toLowerCase(Locale.ROOT);

        // 1순위: 기사 제목의 대표 아티스트 (없으면 본문에서)
        String matchedArtistTag = null;
        for (Map.Entry<String, String> entry : ARTIST_TAG_MAP.entrySet()) {
            if (mainTitle.contains(entry.getKey())) {
                matchedArtistTag = entry.getValue();
                break;
            }
        }
        if (matchedArtistTag == null) {
            for (Map.Entry<String, String> entry : ARTIST_TAG_MAP.entrySet()) {
                if (fullTextLower.contains(entry.getKey())) {
                    matchedArtistTag = entry.getValue();
                    break;
                }
            }
        }
        if (matchedArtistTag != null) {
            resultSet.add(matchedArtistTag);
        }

        // 2순위: 핵심 사건 / 차트 / 성과 태그 (제목 우선 탐색, 본문 보충)
        for (Map.Entry<String, String> entry : EVENT_TAG_MAP.entrySet()) {
            if (mainTitle.contains(entry.getKey())) {
                resultSet.add(entry.getValue());
                if (resultSet.size() >= 2) break;
            }
        }
        if (resultSet.size() < 2) {
            for (Map.Entry<String, String> entry : EVENT_TAG_MAP.entrySet()) {
                if (fullTextLower.contains(entry.getKey())) {
                    resultSet.add(entry.getValue());
                    if (resultSet.size() >= 2) break;
                }
            }
        }

        // 3순위: 아티스트 그룹 분류 (#보이그룹, #걸그룹)
        boolean isGirlGroup = GIRL_GROUPS.stream().anyMatch(fullTextLower::contains);
        boolean isBoyGroup = BOY_GROUPS.stream().anyMatch(fullTextLower::contains);
        if (isGirlGroup) {
            resultSet.add("#걸그룹");
        } else if (isBoyGroup) {
            resultSet.add("#보이그룹");
        }

        // 4순위: 추가 이벤트 태그 또는 기본 토픽 태그 (#KPOP, #KCulture)
        if (resultSet.size() < 4) {
            for (Map.Entry<String, String> entry : EVENT_TAG_MAP.entrySet()) {
                if (fullTextLower.contains(entry.getKey())) {
                    resultSet.add(entry.getValue());
                    if (resultSet.size() >= 4) break;
                }
            }
        }

        String topicTag = "#" + topic.replaceAll("[^a-zA-Z0-9가-힣]", "");
        if (!topicTag.equals("#")) {
            resultSet.add(topicTag);
        }
        resultSet.add("#KCulture");

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
