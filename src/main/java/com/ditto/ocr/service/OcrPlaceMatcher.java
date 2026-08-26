package com.ditto.ocr.service;

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

        List<OcrCandidateResponse> candidates = ranked.stream()
                .map(s -> OcrCandidateResponse.builder()
                        .placeId(s.row.getPlaceId())
                        .navigationKey(s.row.getNavigationKey())
                        .name(s.row.getName())
                        .floor(s.row.getFloor())
                        .confidence(s.wordConfidence)
                        .matchScore(s.matchScore)
                        .build())
                .toList();
        return new MatchResult(ranked.get(0).sourceText, candidates);
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
                bestByPlace.put(row.getPlaceId(), new Scored(row, confidence, score, sourceText));
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

        private Scored(CandidateRow row, double wordConfidence, double matchScore, String sourceText) {
            this.row = row;
            this.wordConfidence = wordConfidence;
            this.matchScore = matchScore;
            this.sourceText = sourceText;
        }
    }

    @Getter
    public static class MatchResult {
        private final String recognizedBrandName;
        private final List<OcrCandidateResponse> candidates;

        public MatchResult(String recognizedBrandName, List<OcrCandidateResponse> candidates) {
            this.recognizedBrandName = recognizedBrandName;
            this.candidates = candidates == null ? List.of() : candidates;
        }

        public static MatchResult empty() {
            return new MatchResult(null, List.of());
        }
    }
}
