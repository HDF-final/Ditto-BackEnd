package com.ditto.ocr.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 간판 영문 표기 → 카탈로그(한글) 상호 별칭 사전.
 *
 * <p>OCR 은 간판에 적힌 영어(EATALY)를 읽지만 카탈로그는 한글(이탈리)로 관리된다. 표시 이름은
 * 그대로 두고, 인식만 언어에 무관하게 이어주기 위한 매핑을 코드로 둔다. 값(한글)은 place 상호에
 * 부분 포함되는 핵심 토큰으로 두어(예: NIKE→나이키 는 "나이키 라이즈" 에도 걸린다) LIKE 매칭에 쓴다.
 */
public final class BrandAliasDictionary {

    private BrandAliasDictionary() {
    }

    /** 영문 별칭 → 한글 상호 핵심 토큰. 표기가 여럿이면 항목을 추가한다. */
    private static final Map<String, String> ENG_TO_KO = Map.ofEntries(
            Map.entry("ADIDAS", "아디다스"),
            Map.entry("NIKE", "나이키"),
            Map.entry("POP MART", "팝마트"),
            Map.entry("STARBUCKS", "스타벅스"),
            Map.entry("NEW BALANCE", "뉴발란스"),
            Map.entry("OAKBERRY", "오크베리"),
            Map.entry("FIVE GUYS", "파이브가이즈"),
            Map.entry("GODIVA", "고디바"),
            Map.entry("PARIS CROISSANT", "파리크라상"),
            Map.entry("LONDON BAGEL MUSEUM", "런던베이글뮤지엄"),
            Map.entry("BALENCIAGA", "발렌시아가"),
            Map.entry("TIFFANY", "티파니"),
            Map.entry("LOEWE", "로에베"),
            Map.entry("GUCCI", "구찌"),
            Map.entry("CELINE", "셀린느"),
            Map.entry("MONCLER", "몽클레르"),
            Map.entry("DIOR", "디올"),
            Map.entry("PRADA", "프라다"),
            Map.entry("BURBERRY", "버버리"),
            Map.entry("BVLGARI", "불가리"),
            Map.entry("FENDI", "펜디"),
            Map.entry("BOTTEGA VENETA", "보테가베네타"),
            Map.entry("LOUIS VUITTON", "루이비통"),
            Map.entry("AESOP", "이솝"),
            Map.entry("SULWHASOO", "설화수"),
            Map.entry("TAMBURINS", "템버린즈"),
            Map.entry("ACNE STUDIOS", "아크네"),
            Map.entry("LANVIN", "랑방"),
            Map.entry("MAISON MARGIELA", "마르지엘라"),
            Map.entry("OUR LEGACY", "아워레가시"),
            Map.entry("MONTBLANC", "몽블랑"),
            Map.entry("TAG HEUER", "태그호이어"),
            Map.entry("CHROME HEARTS", "크롬하츠"),
            Map.entry("KENZO", "겐조"),
            Map.entry("COMME DES GARCONS", "꼼데가르송"),
            Map.entry("SWAROVSKI", "스와로브스키"),
            Map.entry("STONE ISLAND", "스톤아일랜드"),
            Map.entry("MAISON KITSUNE", "메종키츠네"),
            Map.entry("LULULEMON", "룰루레몬"),
            Map.entry("MANDARINA DUCK", "만다리나덕"),
            Map.entry("HAZZYS", "헤지스"),
            Map.entry("LACOSTE", "라코스테"),
            Map.entry("SALOMON", "살로몬"),
            Map.entry("ARCTERYX", "아크테릭스"),
            Map.entry("PATAGONIA", "파타고니아"),
            Map.entry("NORTH FACE", "노스페이스"),
            Map.entry("ZINUS", "지누스"),
            Map.entry("WILLIAMS SONOMA", "윌리엄스소노마"),
            Map.entry("LIVART", "리바트"),
            Map.entry("WILSON", "윌슨"),
            Map.entry("HERMAN MILLER", "허먼밀러"),
            Map.entry("GARMIN", "가민"),
            Map.entry("SNOW PEAK", "스노우피크"),
            Map.entry("LEGO", "레고"),
            Map.entry("DISNEY", "디즈니"),
            Map.entry("DYSON", "다이슨"),
            Map.entry("APPLE", "애플"),
            Map.entry("SAMSUNG", "삼성"),
            Map.entry("BLUE BOTTLE", "블루보틀"),
            Map.entry("EATALY", "이탈리"),
            Map.entry("SANRIO", "산리오"));

    /**
     * OCR 정규화 텍스트가 어떤 영문 별칭을 포함하면, 그 별칭의 한글 상호(정규화)를 검색어로 돌려준다.
     * 별칭 항목을 순회하며 포함 여부를 확인한다.
     */
    public static List<String> canonicalTerms(String normalizedOcr) {
        if (normalizedOcr == null || normalizedOcr.isEmpty()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (Map.Entry<String, String> entry : ENG_TO_KO.entrySet()) {
            String aliasKey = OcrTextNormalizer.normalize(entry.getKey());
            if (!aliasKey.isEmpty() && normalizedOcr.contains(aliasKey)) {
                terms.add(OcrTextNormalizer.normalize(entry.getValue()));
            }
        }
        return terms;
    }
}
