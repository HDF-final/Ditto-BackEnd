package com.ditto.ocr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;
import com.ditto.ocr.dto.response.OcrCandidateResponse;
import com.ditto.ocr.repository.OcrPlaceMapper;
import com.ditto.ocr.repository.OcrPlaceMapper.CandidateRow;
import com.ditto.ocr.support.BrandAliasDictionary;
import com.ditto.ocr.support.OcrTextNormalizer;
import com.ditto.ocr.support.TextSimilarity;

import lombok.RequiredArgsConstructor;

/**
 * OCR 인식 텍스트 → 장소 후보 매칭·랭킹.
 *
 * <p>파이프라인: (1) 상위 N개 조각을 브랜드 후보로 추림 → (2) 각 후보를 정규화하고, 영문 간판
 * 별칭은 {@link BrandAliasDictionary} 로 한글 상호로 바꿔 검색어를 만듦 → (3) 각 검색어로 상호를
 * 정규화 매칭 조회 → (4) 유사도 × OCR 신뢰도로 점수화 → (5) placeId 로 중복 제거 후 점수 내림차순
 * 상위 K개를 돌려준다. 단일 최대 글자 + 단순 LIKE 보다 노이즈·다국어에 강하다.
 */
@Component
@RequiredArgsConstructor
public class OcrPlaceMatcher {

    private final OcrPlaceMapper ocrPlaceMapper;

    public List<OcrCandidateResponse> match(ClovaOcrResult result, int topNWords, int maxCandidates) {
        Map<Long, Scored> bestByPlace = new LinkedHashMap<>();

        List<RecognizedWord> words = result.getWords().stream()
                .filter(word -> !OcrTextNormalizer.normalize(word.getText()).isEmpty())
                .limit(Math.max(topNWords, 1))
                .toList();

        List<String> tokens = words.stream()
                .map(word -> OcrTextNormalizer.normalize(word.getText()))
                .toList();
        double combinedConfidence = words.stream()
                .mapToDouble(RecognizedWord::getConfidence)
                .max()
                .orElse(0.0);
        // POP + MART 처럼 단어가 쪼개져도 POP MART 별칭으로 한글 상호를 찾는다.
        addMatches(bestByPlace, BrandAliasDictionary.canonicalTerms(tokens), combinedConfidence, maxCandidates);

        for (RecognizedWord word : words) {
            String normalizedWord = OcrTextNormalizer.normalize(word.getText());

            // 상호 직접 검색어 + 한 조각 안의 영문 별칭(POP MART / POPMART).
            List<String> searchTerms = new ArrayList<>();
            searchTerms.add(normalizedWord);
            searchTerms.addAll(BrandAliasDictionary.canonicalTerms(normalizedWord));
            addMatches(bestByPlace, searchTerms, word.getConfidence(), maxCandidates);
        }

        return bestByPlace.values().stream()
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .limit(maxCandidates)
                .map(s -> OcrCandidateResponse.builder()
                        .placeId(s.row.getPlaceId())
                        .navigationKey(s.row.getNavigationKey())
                        .name(s.row.getName())
                        .floor(s.row.getFloor())
                        .confidence(s.wordConfidence)
                        .build())
                .toList();
    }

    private void addMatches(Map<Long, Scored> bestByPlace, List<String> searchTerms,
                            double confidence, int maxCandidates) {
        for (String term : searchTerms) {
            if (term == null || term.isEmpty()) {
                continue;
            }
            List<CandidateRow> rows = ocrPlaceMapper.findCandidatesByNormalizedName(term, maxCandidates);
            for (CandidateRow row : rows) {
                double matchScore = score(term, row.getName(), confidence);
                Scored current = bestByPlace.get(row.getPlaceId());
                if (current == null || matchScore > current.score) {
                    bestByPlace.put(row.getPlaceId(), new Scored(row, confidence, matchScore));
                }
            }
        }
    }

    /** 유사도(포함/편집거리)에 OCR 신뢰도를 가중한 점수. 유사도가 지배하고 신뢰도는 보정한다. */
    static double score(String normalizedWord, String placeName, double confidence) {
        double similarity = TextSimilarity.similarity(normalizedWord, OcrTextNormalizer.normalize(placeName));
        return similarity * (0.5 + 0.5 * confidence);
    }

    private static class Scored {
        private final CandidateRow row;
        private final double wordConfidence;
        private final double score;

        private Scored(CandidateRow row, double wordConfidence, double score) {
            this.row = row;
            this.wordConfidence = wordConfidence;
            this.score = score;
        }
    }
}
