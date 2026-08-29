package com.ditto.ocr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.client.RecognizedWord;
import com.ditto.ocr.config.OcrProperties;
import com.ditto.ocr.dto.response.OcrCandidateResponse;
import com.ditto.ocr.repository.OcrPlaceMapper;
import com.ditto.ocr.repository.OcrPlaceMapper.CandidateRow;
import com.ditto.ocr.support.OcrTextNormalizer;
import com.ditto.ocr.support.TextSimilarity;

/**
 * OCR 매칭 파이프라인의 노이즈 내성·랭킹 검증.
 * DB 는 카탈로그 전체를 돌려주는 인메모리 매퍼 스텁으로 대체한다.
 */
class OcrPlaceMatcherTest {

    private OcrPlaceMatcher matcherWith(CandidateRow... rows) {
        List<CandidateRow> catalog = List.of(rows);
        OcrPlaceMapper stub = new OcrPlaceMapper() {
            @Override
            public String findNavigationKeyByPlaceId(Long placeId) {
                return null;
            }

            @Override
            public List<CandidateRow> findAllNavigablePlaces() {
                return catalog;
            }
        };
        return new OcrPlaceMatcher(stub, new OcrProperties());
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
        assertThat(result.get(0).getMatchScore()).isEqualTo(1.0);
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
    @DisplayName("SALE 만 있으면 카탈로그에 매칭하지 않는다")
    void saleAloneDoesNotMatch() {
        OcrPlaceMatcher matcher = matcherWith(row(20L, "TAMBURINS"), row(11L, "EATALY"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("SALE", 0.99, 5000)), 3, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("세일중은 단어 리스트가 아니라 카탈로그 불일치로 버려진다")
    void saleJungDoesNotNeedAStopwordList() {
        OcrPlaceMatcher matcher = matcherWith(row(20L, "TAMBURINS"), row(11L, "EATALY"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("세일중", 0.99, 5000)), 3, 5);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("가장 큰 글자가 세일중이어도 카탈로그에 있는 브랜드가 대표명이 된다")
    void promoDoesNotBecomeRecognizedBrand() {
        OcrPlaceMatcher matcher = matcherWith(row(20L, "TAMBURINS"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(
                        new RecognizedWord("세일중", 0.99, 8000),
                        new RecognizedWord("SALE", 0.98, 7000),
                        new RecognizedWord("TAMBURINS", 0.91, 2000)),
                5);

        assertThat(result.getRecognizedBrandName()).isEqualTo("TAMBURINS");
        assertThat(result.getCandidates()).extracting(OcrCandidateResponse::getPlaceId)
                .containsExactly(20L);
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
        double exact = OcrPlaceMatcher.matchScore("EATALY", "EATALY");
        double fuzzy = OcrPlaceMatcher.matchScore("EATALX", "EATALY");
        assertThat(exact).isGreaterThan(fuzzy);
        assertThat(fuzzy).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("한 글자 오인식은 편집거리 유사도로 높게 유지된다")
    void oneCharTypoStaysSimilar() {
        assertThat(TextSimilarity.similarity("EATALX", "EATALY")).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("LIKE 로는 못 찾는 OCR 오타도 인메모리 fuzzy 로 매칭된다")
    void typoMatchesWithoutLike() {
        OcrPlaceMatcher matcher = matcherWith(row(11L, "EATALY"));
        String typo = OcrTextNormalizer.normalize("EATALX");
        assertThat(OcrTextNormalizer.normalize("EATALY").contains(typo)).isFalse();

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("EATALX", 0.88, 3000)), 3, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceId()).isEqualTo(11L);
        assertThat(result.get(0).getMatchScore()).isGreaterThan(0.8);
        assertThat(result.get(0).getMatchScore()).isLessThan(1.0);
        assertThat(result.get(0).getConfidence()).isEqualTo(0.88);
    }

    @Test
    @DisplayName("영어 오타도 별칭 퍼지로 한글 상호에 매칭되고 matchScore 에 오타가 남는다")
    void englishTypoFuzzyMatchesKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(122L, "이탈리"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("EATALX", 0.91, 3000)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(122L);
        assertThat(result.get(0).getMatchScore()).isGreaterThan(0.8);
        assertThat(result.get(0).getMatchScore()).isLessThan(1.0);
        assertThat(result.get(0).getConfidence()).isEqualTo(0.91);
    }

    @Test
    @DisplayName("exact 매칭은 matchScore 1.0 이고 confidence 는 OCR 값을 그대로 둔다")
    void separatesOcrConfidenceFromMatchScore() {
        OcrPlaceMatcher matcher = matcherWith(row(11L, "EATALY"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("EATALY", 0.73, 3000)), 3, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatchScore()).isEqualTo(1.0);
        assertThat(result.get(0).getConfidence()).isEqualTo(0.73);
    }

    @Test
    @DisplayName("POP MART와 POPMART는 같은 별칭으로 팝마트에 매칭된다")
    void popMartSpacingVariantsMatchSamePlace() {
        OcrPlaceMatcher matcher = matcherWith(row(16L, "팝마트"));

        List<OcrCandidateResponse> spaced = matcher.match(
                words(new RecognizedWord("POP MART", 0.95, 3000)), 3, 5);
        List<OcrCandidateResponse> packed = matcher.match(
                words(new RecognizedWord("POPMART", 0.95, 3000)), 3, 5);

        assertThat(spaced).extracting(OcrCandidateResponse::getPlaceId).containsExactly(16L);
        assertThat(packed).extracting(OcrCandidateResponse::getPlaceId).containsExactly(16L);
        assertThat(spaced.get(0).getMatchScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("POP과 MART가 따로 인식돼도 팝마트에 매칭된다")
    void splitPopMartWordsMatchKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(16L, "팝마트"));

        List<OcrCandidateResponse> result = matcher.match(
                words(
                        new RecognizedWord("POP", 0.94, 2000),
                        new RecognizedWord("MART", 0.93, 1800)),
                3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(16L);
    }

    @Test
    @DisplayName("영어 간판(EATALY)이 별칭 사전으로 한글 상호(이탈리)에 매칭된다")
    void englishSignMatchesKoreanPlaceViaAlias() {
        OcrPlaceMatcher matcher = matcherWith(row(122L, "이탈리"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("EATALY", 0.95, 3000)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(122L);
        assertThat(result.get(0).getMatchScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("별칭이 상호 핵심 토큰이면 접미사가 붙은 상호에도 매칭된다")
    void aliasMatchesPlaceWithSuffix() {
        // NIKE → 나이키, place 는 "나이키 라이즈"
        OcrPlaceMatcher matcher = matcherWith(row(3L, "나이키 라이즈"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("NIKE", 0.9, 4000)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(3L);
    }

    @Test
    @DisplayName("SIE 간판은 한글 상호 시에에 매칭된다")
    void sieMatchesKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(40L, "시에"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("SIE", 0.93, 2500)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(40L);
    }

    @Test
    @DisplayName("TILL I DIE 간판은 틸아이다이에 매칭된다")
    void tillIDieMatchesKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(41L, "틸아이다이"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("TILL I DIE", 0.91, 2800)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(41L);
    }

    @Test
    @DisplayName("LE LABO 간판은 르 라보에 매칭된다")
    void leLaboMatchesKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(42L, "르 라보"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("LE LABO", 0.96, 2200)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(42L);
    }

    @Test
    @DisplayName("NOICE 간판은 노이스에 매칭된다")
    void noiceMatchesKoreanPlace() {
        OcrPlaceMatcher matcher = matcherWith(row(43L, "노이스"));

        List<OcrCandidateResponse> result = matcher.match(
                words(new RecognizedWord("NOICE", 0.94, 2100)), 3, 5);

        assertThat(result).extracting(OcrCandidateResponse::getPlaceId).containsExactly(43L);
    }

    @Test
    @DisplayName("DB에 프라다·프라다뷰티가 둘 다 있고 간판이 프라다면 분기로 사용자가 고른다")
    void pradaAndBeautyBothInCatalogRequireSelection() {
        OcrPlaceMatcher matcher = matcherWith(row(50L, "프라다"), row(51L, "프라다뷰티"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(new RecognizedWord("프라다", 0.95, 3000)), 5);

        assertThat(result.isRequiresSelection()).isTrue();
        assertThat(result.getCandidates()).extracting(OcrCandidateResponse::getPlaceId)
                .containsExactlyInAnyOrder(50L, 51L);
    }

    @Test
    @DisplayName("DB에 뷰티 변형이 없으면 프라다 간판은 분기 없이 바로 답이 된다")
    void pradaWithoutBeautyVariantAnswersDirectly() {
        OcrPlaceMatcher matcher = matcherWith(row(50L, "프라다"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(new RecognizedWord("프라다", 0.95, 3000)), 5);

        assertThat(result.isRequiresSelection()).isFalse();
        assertThat(result.getCandidates()).extracting(OcrCandidateResponse::getPlaceId)
                .containsExactly(50L);
    }

    @Test
    @DisplayName("간판이 프라다뷰티면 프라다도 DB에 있어도 프라다뷰티로 바로 확정한다")
    void beautySignResolvesToBeautyWithoutSelection() {
        OcrPlaceMatcher matcher = matcherWith(row(50L, "프라다"), row(51L, "프라다뷰티"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(new RecognizedWord("프라다뷰티", 0.95, 3000)), 5);

        assertThat(result.isRequiresSelection()).isFalse();
        assertThat(result.getCandidates().get(0).getPlaceId()).isEqualTo(51L);
    }

    @Test
    @DisplayName("영어 간판(PRADA)도 DB에 프라다·프라다뷰티가 둘 다 있으면 분기가 된다")
    void englishPradaSignRequiresSelectionViaAlias() {
        OcrPlaceMatcher matcher = matcherWith(row(50L, "프라다"), row(51L, "프라다뷰티"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(new RecognizedWord("PRADA", 0.95, 3000)), 5);

        assertThat(result.isRequiresSelection()).isTrue();
        assertThat(result.getCandidates()).extracting(OcrCandidateResponse::getPlaceId)
                .containsExactlyInAnyOrder(50L, 51L);
    }

    @Test
    @DisplayName("단일 확정 매칭은 분기가 아니다")
    void singleMatchDoesNotRequireSelection() {
        OcrPlaceMatcher matcher = matcherWith(row(20L, "TAMBURINS"));

        OcrPlaceMatcher.MatchResult result = matcher.resolve(
                words(new RecognizedWord("TAMBURINS", 0.92, 3000)), 5);

        assertThat(result.isRequiresSelection()).isFalse();
        assertThat(result.getCandidates()).hasSize(1);
    }
}
