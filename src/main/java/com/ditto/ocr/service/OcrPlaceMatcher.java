package com.ditto.ocr.service;

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
import com.ditto.ocr.support.OcrTextNormalizer;
import com.ditto.ocr.support.TextSimilarity;

import lombok.RequiredArgsConstructor;

/**
 * OCR 인식 텍스트 → 장소 후보 매칭·랭킹.
 *
 * <p>파이프라인: (1) 상위 N개 조각을 브랜드 후보로 추림 → (2) 각 후보를 정규화해 상호를
 * 정규화 매칭으로 조회 → (3) 유사도 × OCR 신뢰도로 점수화 → (4) placeId 로 중복 제거 후
 * 점수 내림차순 상위 K개를 돌려준다. 단일 최대 글자 + 단순 LIKE 보다 노이즈에 강하다.
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

        for (RecognizedWord word : words) {
            String normalizedWord = OcrTextNormalizer.normalize(word.getText());
            List<CandidateRow> rows = ocrPlaceMapper.findCandidatesByNormalizedName(normalizedWord, maxCandidates);
            for (CandidateRow row : rows) {
                double score = score(normalizedWord, row.getName(), word.getConfidence());
                Scored current = bestByPlace.get(row.getPlaceId());
                if (current == null || score > current.score) {
                    bestByPlace.put(row.getPlaceId(), new Scored(row, word.getConfidence(), score));
                }
            }
        }

        return bestByPlace.values().stream()
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .limit(maxCandidates)
                .map(s -> OcrCandidateResponse.builder()
                        .placeId(s.row.getPlaceId())
                        .name(s.row.getName())
                        .floor(s.row.getFloor())
                        .confidence(s.wordConfidence)
                        .build())
                .toList();
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
