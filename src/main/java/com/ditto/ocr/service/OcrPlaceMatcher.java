package com.ditto.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;
import com.ditto.ocr.config.OcrProperties;
import com.ditto.ocr.dto.response.OcrCandidateResponse;
import com.ditto.ocr.repository.OcrPlaceMapper;
import com.ditto.ocr.repository.OcrPlaceMapper.CandidateRow;
import com.ditto.ocr.support.BrandAliasDictionary;
import com.ditto.ocr.support.OcrStopwords;
import com.ditto.ocr.support.OcrTextNormalizer;
import com.ditto.ocr.support.TextSimilarity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OCR 인식 텍스트 → 장소 후보 매칭·랭킹.
 *
 * <p>프로모 문구는 단어 리스트로 지우지 않는다. 인식된 조각을 카탈로그와 대조해서
 * {@code matchScore} 가 되는 것만 남긴다. {@code 세일중}·{@code SALE} 은 매장이 아니라서
 * 후보가 안 되고, 같은 사진의 브랜드 조각만 살아난다.
 *
 * <p>파이프라인: (1) 구조적 노이즈(층·가격·%)만 제외한 조각을 모두 씀 → (2) 정규화하고
 * 영문 별칭은 {@link BrandAliasDictionary} 로 한글 상호로 바꿈 → (3) exact / alias / fuzzy
 * 점수화 → (4) placeId 중복 제거 후 {@code matchScore} 내림차순 상위 K개.
 *
 * <p>(5) 같은 점수로 걸린 서로 다른 매장이 여럿이면 분기다. 카탈로그(DB)에 프라다·프라다뷰티가
 * 둘 다 있고 간판이 "프라다" 뿐이면 어느 쪽인지 알 수 없으므로 {@code requiresSelection} 을 켜서
 * 사용자가 고르게 한다. 뷰티 변형이 DB 에 없으면 후보가 하나뿐이라 바로 답이 된다.
 *
 * <p>대표 브랜드명은 가장 큰 OCR 글자가 아니라, 실제로 매칭된 조각이다.
 */
@Component
@RequiredArgsConstructor
public class OcrPlaceMatcher {

    private final OcrPlaceMapper ocrPlaceMapper;
    private final OcrProperties properties;

    public List<OcrCandidateResponse> match(ClovaOcrResult result, int topNWords, int maxCandidates) {
        return resolve(result, maxCandidates).getCandidates();
    }

    /**
     * 카탈로그에 붙은 후보와, 그 후보를 만든 OCR 조각을 함께 돌려준다.
     * 대표 브랜드명은 가장 큰 글자(세일중)가 아니라 실제 매칭 조각이다.
     */
    public MatchResult resolve(ClovaOcrResult result, int maxCandidates) {
        List<CandidateRow> catalog = ocrPlaceMapper.findAllNavigablePlaces();
        if (catalog == null || catalog.isEmpty() || result == null || result.isEmpty()) {
            return MatchResult.empty();
        }

        Map<Long, Scored> bestByPlace = new LinkedHashMap<>();

        // 큰 글자 N개만 보면 프로모가 브랜드를 밀어낸다. 카탈로그가 작으므로 남은 조각을 모두 대조한다.
        List<RecognizedWord> words = result.getWords().stream()
                .filter(word -> {
                    String normalized = OcrTextNormalizer.normalize(word.getText());
                    return !normalized.isEmpty() && !OcrStopwords.isStructuralNoise(word.getText());
                })
                .toList();
        if (words.isEmpty()) {
            return MatchResult.empty();
        }

        List<String> tokens = words.stream()
                .map(word -> OcrTextNormalizer.normalize(word.getText()))
                .toList();
        double combinedConfidence = words.stream()
                .mapToDouble(RecognizedWord::getConfidence)
                .max()
                .orElse(0.0);
        // POP + MART 처럼 단어가 쪼개져도 POP MART 별칭으로 한글 상호를 찾는다.
        for (String aliasTerm : BrandAliasDictionary.canonicalTerms(tokens)) {
            addMatches(bestByPlace, catalog, aliasTerm, combinedConfidence, 1.0, aliasTerm);
        }

        OcrProperties.Matching matching = properties.getMatching();
        for (RecognizedWord word : words) {
            String normalizedWord = OcrTextNormalizer.normalize(word.getText());
            addMatches(bestByPlace, catalog, normalizedWord, word.getConfidence(), 1.0, word.getText());
            for (String aliasTerm : BrandAliasDictionary.canonicalTerms(normalizedWord)) {
                addMatches(bestByPlace, catalog, aliasTerm, word.getConfidence(), 1.0, word.getText());
            }
            for (BrandAliasDictionary.AliasHit hit : BrandAliasDictionary.fuzzyCanonicalTerms(
                    normalizedWord, matching.getFuzzyThreshold(), matching.getMinFuzzyLength())) {
                addMatches(bestByPlace, catalog, hit.getKoreanNormalized(),
                        word.getConfidence(), hit.getKeySimilarity(), word.getText());
            }
        }

        double minMatchScore = matching.getMinMatchScore();
        List<Scored> ranked = bestByPlace.values().stream()
                .filter(s -> s.matchScore >= minMatchScore)
                .sorted(Comparator.comparingDouble((Scored s) -> s.matchScore).reversed()
                        .thenComparing(Comparator.comparingDouble((Scored s) -> s.wordConfidence).reversed()))
                .limit(maxCandidates)
                .toList();
        if (ranked.isEmpty()) {
            return MatchResult.empty();
        }

        // 카탈로그(DB)에 같은 점수로 걸린 서로 다른 매장이 여럿이면 분기다.
        // 프라다·프라다뷰티가 둘 다 있으면 사용자가 고르고, 뷰티 변형이 DB에 없으면 바로 답이 된다.
        List<Scored> tied = topScoreGroup(ranked, matching.getSelectionScoreDelta());
        boolean requiresSelection = false;
        List<Scored> outcome = ranked;
        if (tied.size() >= 2) {
            Scored specific = mostSpecificName(tied);
            if (specific != null) {
                // 간판이 이미 가장 구체적인 상호(프라다뷰티)를 담고 있으면 그걸로 확정한다.
                outcome = moveToFront(ranked, specific);
            } else {
                // 프라다 ⊂ 프라다뷰티 처럼 같은 점수의 다른 매장이면 후보만 남겨 사용자가 고른다.
                requiresSelection = true;
                outcome = tied;
            }
        }

        List<OcrCandidateResponse> candidates = outcome.stream()
                .map(s -> OcrCandidateResponse.builder()
                        .placeId(s.row.getPlaceId())
                        .navigationKey(s.row.getNavigationKey())
                        .name(s.row.getName())
                        .floor(s.row.getFloor())
                        .confidence(s.wordConfidence)
                        .matchScore(s.matchScore)
                        .build())
                .toList();
        return new MatchResult(outcome.get(0).sourceText, requiresSelection, candidates);
    }

    /** 최상위 matchScore 와의 차이가 {@code delta} 이하인 후보들. 완전 동점이면 delta 0 으로 잡힌다. */
    private static List<Scored> topScoreGroup(List<Scored> ranked, double delta) {
        double topScore = ranked.get(0).matchScore;
        return ranked.stream()
                .filter(s -> topScore - s.matchScore <= delta + 1e-9)
                .toList();
    }

    /**
     * 동점 후보 중 "간판이 곧 그 상호"인 후보를 고른다. 카탈로그에서 가장 긴 상호가 유일하고
     * 그 상호가 인식 토큰과 정확히 일치할 때만 반환한다. 프라다뷰티 간판은 프라다뷰티로 확정되고,
     * 프라다 간판(프라다 ⊂ 프라다뷰티)이나 같은 이름의 매장이 둘이면 {@code null} 이라 분기가 된다.
     */
    private static Scored mostSpecificName(List<Scored> tied) {
        int maxLen = tied.stream()
                .mapToInt(s -> OcrTextNormalizer.normalize(s.row.getName()).length())
                .max()
                .orElse(0);
        Scored longestExact = null;
        int longestCount = 0;
        for (Scored s : tied) {
            String name = OcrTextNormalizer.normalize(s.row.getName());
            if (name.length() != maxLen) {
                continue;
            }
            longestCount++;
            if (name.equals(s.matchTerm)) {
                longestExact = s;
            }
        }
        return (longestCount == 1 && longestExact != null) ? longestExact : null;
    }

    private static List<Scored> moveToFront(List<Scored> ranked, Scored pick) {
        List<Scored> reordered = new ArrayList<>(ranked.size());
        reordered.add(pick);
        for (Scored s : ranked) {
            if (s != pick) {
                reordered.add(s);
            }
        }
        return reordered;
    }

    /**
     * {@code scoreCap} 은 exact/alias 는 1.0, 별칭 키를 퍼지로 찾은 경우에는 그 유사도다.
     * OCR 오타가 matchScore 에 남고, OCR confidence 와는 섞지 않는다.
     */
    private void addMatches(Map<Long, Scored> bestByPlace, List<CandidateRow> catalog,
                            String term, double confidence, double scoreCap, String sourceText) {
        if (term == null || term.isEmpty()) {
            return;
        }
        boolean allowFuzzy = term.length() >= properties.getMatching().getMinFuzzyLength();
        double minMatchScore = properties.getMatching().getMinMatchScore();

        for (CandidateRow row : catalog) {
            double similarity = matchScore(term, row.getName());
            if (similarity < 1.0 && !allowFuzzy) {
                continue;
            }
            double score = Math.min(similarity, scoreCap);
            if (score < minMatchScore) {
                continue;
            }
            Scored current = bestByPlace.get(row.getPlaceId());
            if (current == null || score > current.matchScore
                    || (score == current.matchScore && confidence > current.wordConfidence)) {
                bestByPlace.put(row.getPlaceId(), new Scored(row, confidence, score, sourceText, term));
            }
        }
    }

    /** 정규화 상호에 대한 exact·포함·편집거리 유사도. OCR 신뢰도는 넣지 않는다. */
    static double matchScore(String normalizedWord, String placeName) {
        return TextSimilarity.similarity(normalizedWord, OcrTextNormalizer.normalize(placeName));
    }

    private static class Scored {
        private final CandidateRow row;
        private final double wordConfidence;
        private final double matchScore;
        private final String sourceText;
        /** 이 점수를 만든 정규화 매칭어(카탈로그 언어). 프라다 분기 판단에 쓴다. */
        private final String matchTerm;

        private Scored(CandidateRow row, double wordConfidence, double matchScore,
                       String sourceText, String matchTerm) {
            this.row = row;
            this.wordConfidence = wordConfidence;
            this.matchScore = matchScore;
            this.sourceText = sourceText;
            this.matchTerm = matchTerm;
        }
    }

    @Getter
    public static class MatchResult {
        private final String recognizedBrandName;
        /** 같은 점수의 매장이 여럿이라 사용자가 골라야 하면 true(예: 프라다 vs 프라다뷰티). */
        private final boolean requiresSelection;
        private final List<OcrCandidateResponse> candidates;

        public MatchResult(String recognizedBrandName, boolean requiresSelection,
                           List<OcrCandidateResponse> candidates) {
            this.recognizedBrandName = recognizedBrandName;
            this.requiresSelection = requiresSelection;
            this.candidates = candidates == null ? List.of() : candidates;
        }

        public static MatchResult empty() {
            return new MatchResult(null, false, List.of());
        }
    }
}
