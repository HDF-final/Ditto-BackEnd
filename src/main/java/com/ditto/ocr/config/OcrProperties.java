package com.ditto.ocr.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * OCR 길찾기 설정.
 *
 * <p>인식은 네이버 CLOVA OCR(외부 서비스)에 맡기고, 세션은 인메모리로 관리한다.
 * provider 를 바꾸면 {@link com.ditto.ocr.client.ClovaOcrClient} 만 갈아끼우면 된다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.ocr")
public class OcrProperties {

    /** 세션 유효 시간. 이 시간이 지난 세션으로 인식을 요청하면 만료로 처리한다. */
    private Duration sessionTtl = Duration.ofMinutes(30);

    /** 브랜드명 하나로 돌려줄 최대 후보 장소 수. */
    private int maxCandidates = 5;

    /**
     * 예전 매칭이 면적 상위 N개만 보던 한도. 지금은 카탈로그 대조가 프로모를 걸러서
     * 이 값을 매칭에 쓰지 않는다. 설정 호환을 위해 남겨 둔다.
     */
    private int brandTopN = 5;

    private final Preprocess preprocess = new Preprocess();

    private final Matching matching = new Matching();

    private final Clova clova = new Clova();

    private final Concurrency concurrency = new Concurrency();

    /**
     * CLOVA 호출 전 이미지 축소. 스마트폰 원본(수 MB·수천 px)을 그대로 보내면
     * 업로드+인식 latency 가 커진다.
     */
    @Getter
    @Setter
    public static class Preprocess {

        private boolean enabled = true;

        /** 긴 변 최대 픽셀. 이보다 크면 비율을 유지한 채 줄인다. */
        private int maxLongSidePx = 1600;

        /** 재인코딩 후 목표 상한(바이트). 넘으면 JPEG 품질을 한 번 더 낮춘다. */
        private int maxBytes = 1_048_576;

        /** JPEG 품질(0~1). */
        private float jpegQuality = 0.82f;

        /**
         * 디코딩을 허용하는 최대 픽셀 수(width×height). 디컴프레션 밤 방어용.
         *
         * <p>업로드 바이트가 작아도 디코딩하면 거대한 이미지로 풀리는 공격을 막는다.
         * ImageIO 로 전체를 읽기 전에 헤더 크기를 먼저 검사해 초과 시 거절한다.
         * TYPE_INT_RGB 는 픽셀당 4바이트라 24MP ≈ 96MB/장 —
         * {@code concurrency.maxConcurrent} 와 곱해 최악 힙 사용량을 가늠한다.
         */
        private long maxDecodePixels = 24_000_000L;
    }

    /**
     * 카탈로그 인메모리 엔티티 매칭. SQL LIKE 대신 exact / alias / fuzzy 를 쓴다.
     */
    @Getter
    @Setter
    public static class Matching {

        /** fuzzy 후보로 인정하는 최소 유사도(0~1). */
        private double fuzzyThreshold = 0.8;

        /** 이보다 짧은 토큰은 오타 보정 없이 exact·alias 만 본다. */
        private int minFuzzyLength = 4;

        /** 응답에 올릴 최소 matchScore. OCR 신뢰도({@code confidence})와는 별개다. */
        private double minMatchScore = 0.8;

        /**
         * 분기(선택) 판단에 쓰는 최상위 점수와의 허용 격차.
         *
         * <p>최상위 matchScore 와의 차이가 이 값 이하인 후보들을 "같은 점수"로 보고,
         * 그런 후보가 서로 다른 매장으로 2개 이상이면 사용자가 고르도록 {@code requiresSelection}
         * 을 켠다(예: 프라다 vs 프라다뷰티). 0.0 이면 완전 동점만 분기로 본다.
         */
        private double selectionScoreDelta = 0.0;
    }

    /** 네이버 CLOVA OCR(General) 접속 설정. */
    @Getter
    @Setter
    public static class Clova {

        /** APIGW Invoke URL(도메인별 엔드포인트 전체 URL). 자격정보라 소스에 두지 않는다. */
        private String invokeUrl;

        /** CLOVA OCR Secret Key. {@code X-OCR-SECRET} 헤더로 보낸다. */
        private String secret;

        private Duration connectTimeout = Duration.ofSeconds(5);

        private Duration readTimeout = Duration.ofSeconds(30);
    }

    /**
     * OCR 무거운 구간(이미지 디코딩 + CLOVA 호출) 동시 처리 제한(벌크헤드).
     *
     * <p>외부 호출은 요청 스레드를 수 초 붙잡는다. 동시 진입 수를 Tomcat 워커 풀보다
     * 작게 묶어, 폭주 시에도 OCR 이 전체 스레드를 먹고 앱 전체를 멈추게 하는 일을 막는다.
     */
    @Getter
    @Setter
    public static class Concurrency {

        /**
         * 동시에 OCR 무거운 구간에 들어갈 수 있는 최대 요청 수.
         * Tomcat max-threads(기본 200)보다 훨씬 작게 잡아 스레드 독점을 막는다.
         */
        private int maxConcurrent = 20;

        /**
         * 허가 대기 최대 시간. 이 시간 안에 슬롯을 못 얻으면 즉시 503.
         * 짧게 잡아야 초과 요청이 워커 스레드를 오래 붙잡지 않는다.
         */
        private Duration acquireTimeout = Duration.ofMillis(200);
    }
}
