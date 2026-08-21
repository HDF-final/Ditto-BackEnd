package com.ditto.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;
import com.ditto.ocr.dto.response.OcrCandidateResponse;
import com.ditto.ocr.repository.OcrPlaceMapper;
import com.ditto.ocr.repository.OcrPlaceMapper.CandidateRow;
import com.ditto.ocr.support.OcrTextNormalizer;
import com.ditto.ocr.support.TextSimilarity;

/**
 * OCR 매칭 파이프라인의 노이즈 내성·랭킹 검증.
 * DB 는 정규화 규칙을 흉내 낸 인메모리 매퍼 스텁으로 대체한다.
 */
class OcrPlaceMatcherTest {

    private OcrPlaceMatcher matcherWith(CandidateRow... rows) {
        OcrPlaceMapper stub = new OcrPlaceMapper() {
            @Override
            public String findNavigationKeyByPlaceId(Long placeId) {
                return null;
            }

            @Override
            public List<CandidateRow> findCandidatesByNormalizedName(String norm, int limit) {
                // 실제 SQL 의 정규화 LIKE 를 흉내 낸다.
                return List.of(rows).stream()
                        .filter(r -> OcrTextNormalizer.normalize(r.getName()).contains(norm))
                        .limit(limit)
                        .toList();
            }
        };
        return new OcrPlaceMatcher(stub);
    }

    private CandidateRow row(long id, String name) {
        CandidateRow r = new CandidateRow();
        r.setPlaceId(id);
        r.setName(name);
        r.setFloor("1F");
        return r;
    }

    private ClovaOcrResult words(RecognizedWord... words) {
        return new ClovaOcrResult(List.of(words));
    }

    @Test
    @DisplayName("띄어쓰기·기호가 섞인 OCR 텍스트도 정규화로 상호에 매칭된다")
    void normalizesNoisyText() {
        OcrPlaceMatcher matcher = matcherWith(row(11L, "EATALY"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("EAT ALY.", 0.9, 1000)), 3, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("브랜드가 최대 글자가 아니어도 상위 여러 조각에서 매칭된다")
    void matchesFromLowerRankedWord() {
        OcrPlaceMatcher matcher = matcherWith(row(20L, "TAMBURINS"));

        // 가장 큰 글자는 프로모 문구, 두 번째가 실제 브랜드
        List<OcrCandidateResponse> result = matcher.match(
                words(
                        new RecognizedWord("SALE", 0.95, 5000),
                        new RecognizedWord("TAMBURINS", 0.92, 3000)),
                3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).contains(20L);
    }

    @Test
    @DisplayName("여러 조각이 같은 장소를 가리키면 중복 없이 한 번만 나온다")
    void dedupesByPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(30L, "OLIVE YOUNG"));

        List<OcrCandidateResponse> result = matcher.match(
                words(
                        new RecognizedWord("OLIVE", 0.9, 2000),
                        new RecognizedWord("YOUNG", 0.9, 1800)),
                3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(30L);
    }

    @Test
    @DisplayName("포함 매칭이 오탈자 근접 매칭보다 높은 점수를 받는다")
    void containmentScoresHigherThanFuzzy() {
        double exact = OcrPlaceMatcher.score("EATALY", "EATALY", 1.0);
        double fuzzy = OcrPlaceMatcher.score("EATALX", "EATALY", 1.0);
        assertThat(exact).isGreaterThan(fuzzy);
        assertThat(fuzzy).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("한 글자 오인식은 편집거리 유사도로 높게 유지된다")
    void oneCharTypoStaysSimilar() {
        assertThat(TextSimilarity.similarity("EATALX", "EATALY")).isGreaterThan(0.8);
    }
}
