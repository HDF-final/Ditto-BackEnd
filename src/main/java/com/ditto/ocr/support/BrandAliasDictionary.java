package com.ditto.ocr.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final Map<String, String> ENG_TO_KO = buildDictionary();

    private static Map<String, String> buildDictionary() {
        Map<String, String> map = new LinkedHashMap<>();

        // ===== 기존 매핑 =====
        map.put("ADIDAS", "아디다스");
        map.put("NIKE", "나이키");
        map.put("POP MART", "팝마트");
        map.put("STARBUCKS", "스타벅스");
        map.put("NEW BALANCE", "뉴발란스");
        map.put("OAKBERRY", "오크베리");
        map.put("FIVE GUYS", "파이브가이즈");
        map.put("GODIVA", "고디바");
        map.put("PARIS CROISSANT", "파리크라상");
        map.put("LONDON BAGEL MUSEUM", "런던베이글뮤지엄");
        map.put("BALENCIAGA", "발렌시아가");
        map.put("TIFFANY", "티파니");
        map.put("LOEWE", "로에베");
        map.put("GUCCI", "구찌");
        map.put("CELINE", "셀린느");
        map.put("MONCLER", "몽클레르");
        map.put("DIOR", "디올");
        map.put("PRADA", "프라다");
        map.put("BURBERRY", "버버리");
        map.put("BVLGARI", "불가리");
        map.put("FENDI", "펜디");
        map.put("BOTTEGA VENETA", "보테가베네타");
        map.put("LOUIS VUITTON", "루이비통");
        map.put("AESOP", "이솝");
        map.put("SULWHASOO", "설화수");
        map.put("TAMBURINS", "템버린즈");
        map.put("ACNE STUDIOS", "아크네");
        map.put("LANVIN", "랑방");
        map.put("MAISON MARGIELA", "마르지엘라");
        map.put("OUR LEGACY", "아워레가시");
        map.put("MONTBLANC", "몽블랑");
        map.put("TAG HEUER", "태그호이어");
        map.put("CHROME HEARTS", "크롬하츠");
        map.put("KENZO", "겐조");
        map.put("COMME DES GARCONS", "꼼데가르송");
        map.put("SWAROVSKI", "스와로브스키");
        map.put("STONE ISLAND", "스톤아일랜드");
        map.put("MAISON KITSUNE", "메종키츠네");
        map.put("LULULEMON", "룰루레몬");
        map.put("MANDARINA DUCK", "만다리나덕");
        map.put("HAZZYS", "헤지스");
        map.put("LACOSTE", "라코스테");
        map.put("SALOMON", "살로몬");
        map.put("ARCTERYX", "아크테릭스");
        map.put("PATAGONIA", "파타고니아");
        map.put("NORTH FACE", "노스페이스");
        map.put("ZINUS", "지누스");
        map.put("WILLIAMS SONOMA", "윌리엄스소노마");
        map.put("LIVART", "리바트");
        map.put("WILSON", "윌슨");
        map.put("HERMAN MILLER", "허먼밀러");
        map.put("GARMIN", "가민");
        map.put("SNOW PEAK", "스노우피크");
        map.put("LEGO", "레고");
        map.put("DISNEY", "디즈니");
        map.put("DYSON", "다이슨");
        map.put("APPLE", "애플");
        map.put("SAMSUNG", "삼성");
        map.put("BLUE BOTTLE", "블루보틀");
        map.put("EATALY", "이탈리");
        map.put("SANRIO", "산리오");
        map.put("SYSTEM", "시스템");
        map.put("SYSTEM HOMME", "시스템옴므");

        // ===== 기존 브랜드 영문 표기 변형 =====
        map.put("THE NORTH FACE", "노스페이스");
        map.put("BULGARI", "불가리");
        map.put("ARC TERYX", "아크테릭스");
        map.put("TIFFANY AND CO", "티파니");
        map.put("TIFFANY CO", "티파니");
        map.put("CHRISTIAN DIOR", "크리스챤디올");
        map.put("COMME DES GARCON", "꼼데가르송");
        map.put("ACNE STUDIO", "아크네");
        map.put("STARBUCKS RESERVE", "스타벅스");
        map.put("NIKE RISE", "나이키");
        map.put("ADIDAS STADIUM", "아디다스");
        map.put("GODIVA BAKERY", "고디바");
        map.put("APPLE STORE", "애플");
        map.put("SAMSUNG STORE", "삼성");
        map.put("DISNEY STORE", "디즈니");
        map.put("LEGO LCS", "레고");
        map.put("EATALY MARKET", "이탈리");
        map.put("POLO", "폴로");
        map.put("POLO RALPH LAUREN", "폴로");
        map.put("RALPH LAUREN", "폴로");

        // ===== 패션·라이프스타일 =====
        map.put("LMC", "LMC");
        map.put("SIE", "시에");
        map.put("NO MANUAL", "노메뉴얼");
        map.put("SANSAN GEAR", "산산기어");
        map.put("SAN SAN GEAR", "산산기어");
        map.put("POINT OF VIEW", "포인트오브뷰");
        map.put("TILL I DIE", "틸아이다이");
        map.put("TILLIDIE", "틸아이다이");
        map.put("NOICE", "노이스");
        map.put("ON RUNNING", "온");
        map.put("TIME", "타임");
        map.put("MINE", "마인");
        map.put("SIDAS", "시다스");
        map.put("ARENA", "아레나");
        map.put("GOOD RUNNER", "굿러너");
        map.put("GOOD RUNNER COMPANY", "굿러너");
        map.put("WE PET", "WEPET");
        map.put("WEPET", "WEPET");
        map.put("THE HYUNDAI SOUVENIR", "더현대수비니어");
        map.put("HYUNDAI SOUVENIR", "더현대수비니어");
        map.put("PLAY IN THE BOX", "플레이인더박스");
        map.put("LG MEGASTORE", "LG");
        map.put("EPIC SEOUL", "에픽서울");
        map.put("29CM", "29CM");
        map.put("29CM HOME", "29CM");
        map.put("MLB", "MLB");
        map.put("PXG", "PXG");
        map.put("MAC", "MAC");

        // ===== F&B =====
        map.put("ETF BAKERY", "ETF베이커리");
        map.put("ETF", "ETF베이커리");
        map.put("WINE WORKS", "WINEWORKS");
        map.put("DOJO COFFEE", "도조커피");
        map.put("DOJO", "도조커피");
        map.put("COCOBAP", "코코밥");
        map.put("COCO BAP", "코코밥");
        map.put("BONGA SUSHI", "본가스시");
        map.put("BEATLES TACO", "비틀스타코");
        map.put("MAYUYU", "마유유");
        map.put("MA YU YU", "마유유");
        map.put("THE RAMEN WAR", "더라멘워");
        map.put("RAMEN WAR", "더라멘워");
        map.put("BAPGUBNAM", "밥굽남");
        map.put("GANGHOYEONPA", "강호연파");
        map.put("TONKATSU 1985", "돈까스1985");
        map.put("DONKATSU 1985", "돈까스1985");
        map.put("HOWOOSEOM", "호우섬");
        map.put("ARTIST BAKERY", "아티스트베이커리");
        map.put("SEOUL MANDU", "서울만두");
        map.put("SEOUL DUMPLING", "서울만두");
        map.put("PINKS HOT DOG", "핑크스");
        map.put("PINK'S", "핑크스");
        map.put("PINKS", "핑크스");
        map.put("22 FOOD TRUCK", "22푸드트럭");
        map.put("FOOD TRUCK PIZZA", "22푸드트럭");
        map.put("GOLDEN CHEESE", "골든치즈");
        map.put("SMT LOUNGE", "SMT");
        map.put("BUN PATTY BUN", "번패티번");
        map.put("CAFE H", "카페H");
        map.put("DOWON STYLE", "도원스타일");
        map.put("ROVA", "로바");
        map.put("LOBA", "로바");
        map.put("SONG", "송");
        map.put("JEONGDON", "정돈");
        map.put("JUNG DON", "정돈");
        map.put("RISTORANTE EO", "리스토란테에오");
        map.put("GAYA", "가야");
        map.put("YOJEUM GIMBAP", "요즘김밥");
        map.put("QUEEN TTEOKBOKKI", "여왕떡볶이");
        map.put("YEOWANG", "여왕떡볶이");
        map.put("JEONJU SEON", "전주선비빔");
        map.put("HANSOL", "한솔냉면");
        map.put("SHABU MIDAM", "샤브미담");
        map.put("HANJEONGSEON", "한정선");
        map.put("SAMSEONGHYEOL", "삼성혈");
        map.put("TEAMFIGHT TACTICS", "TFT");
        map.put("TFT", "TFT");
        map.put("K LEAGUE", "K리그");

        // ===== 뷰티 =====
        map.put("LE LABO", "르라보");
        map.put("DIPTYQUE", "딥디크");
        map.put("ESTEE LAUDER", "에스티로더");
        map.put("YSL", "입생로랑");
        map.put("YVES SAINT LAURENT", "입생로랑");
        map.put("BYREDO", "바이레도");
        map.put("CHANEL", "샤넬");
        map.put("CHANEL BEAUTY", "샤넬");
        map.put("HERMES", "에르메스");
        map.put("HERMES BEAUTY", "에르메스");
        map.put("PRADA BEAUTY", "프라다뷰티");
        map.put("ARMANI", "아르마니");
        map.put("ARMANI BEAUTY", "아르마니");
        map.put("GIORGIO ARMANI", "아르마니");
        map.put("DIOR BEAUTY", "디올뷰티");
        map.put("OHUI", "오휘");
        map.put("O HUI", "오휘");
        map.put("TOM FORD", "톰포드");
        map.put("LOCCITANE", "록시땅");
        map.put("L OCCITANE", "록시땅");
        map.put("JO MALONE", "조말론");
        map.put("KIEHLS", "키엘");
        map.put("KIEHL'S", "키엘");
        map.put("HERA", "헤라");

        return Map.copyOf(map);
    }

    /**
     * OCR 정규화 텍스트가 어떤 영문 별칭을 포함하면, 그 별칭의 한글 상호(정규화)를 검색어로 돌려준다.
     *
     * <p>띄어쓰기는 정규화 단계에서 빠지므로 {@code POP MART} 와 {@code POPMART} 는 같다.
     * CLOVA 가 단어를 따로 주면 {@link #canonicalTerms(List)} 로 조각을 모아 맞춘다.
     */
    public static List<String> canonicalTerms(String normalizedOcr) {
        if (normalizedOcr == null || normalizedOcr.isEmpty()) {
            return List.of();
        }
        return canonicalTerms(List.of(normalizedOcr));
    }

    /**
     * 인식 조각 여러 개로 별칭을 찾는다. {@code POP} + {@code MART} 처럼 간판 단어가
     * 쪼개져도 {@code POP MART} 별칭에 매칭된다.
     */
    public static List<String> canonicalTerms(List<String> normalizedTokens) {
        if (normalizedTokens == null || normalizedTokens.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String token : normalizedTokens) {
            if (token != null && !token.isEmpty()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<String> terms = new ArrayList<>();
        for (Map.Entry<String, String> entry : ENG_TO_KO.entrySet()) {
            if (!matchesAlias(entry.getKey(), tokens)) {
                continue;
            }
            String korean = OcrTextNormalizer.normalize(entry.getValue());
            if (!korean.isEmpty() && !terms.contains(korean)) {
                terms.add(korean);
            }
        }
        return terms;
    }

    /**
     * 한 조각이 별칭 전체(POPMART)를 포함하거나, 별칭의 각 단어(POP, MART)가
     * 조각들 안에 모두 있으면 매칭으로 본다.
     */
    private static boolean matchesAlias(String rawAlias, List<String> tokens) {
        String aliasKey = OcrTextNormalizer.normalize(rawAlias);
        if (aliasKey.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (token.contains(aliasKey)) {
                return true;
            }
        }

        String[] parts = rawAlias.trim().split("\\s+");
        if (parts.length < 2) {
            return false;
        }
        for (String part : parts) {
            String normalizedPart = OcrTextNormalizer.normalize(part);
            if (normalizedPart.isEmpty() || !containsPart(tokens, normalizedPart)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsPart(List<String> tokens, String part) {
        for (String token : tokens) {
            if (token.equals(part)) {
                return true;
            }
            // 한두 글자(ON, H)는 부분 포함이면 오탐이 나서 완전 일치만 허용한다.
            if (part.length() >= 3 && token.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
