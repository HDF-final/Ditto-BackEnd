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

    /** 매칭에 쓸 상위 인식 텍스트 조각 수(도드라짐 순). 브랜드가 최대 글자가 아닐 때를 대비한다. */
    private int brandTopN = 3;

    private final Clova clova = new Clova();

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
}
