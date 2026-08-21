package com.ditto.mobile.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 모바일 접속(접속 코드) 설정.
 *
 * <p>접속 코드는 재배포·다중 인스턴스에도 유지되도록 Oracle 에 저장한다. 만료 시간(아래 TTL)이
 * 지나면 조회 시점(지연 만료)과 주기적 청소로 함께 걷어낸다.
 * ({@link com.ditto.mobile.service.MobileAccessService},
 * {@link com.ditto.mobile.service.MobileExpiryCleaner})
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.mobile")
public class MobileProperties {

    /** 접속 코드 유효 시간(기본 30일). 이 시간이 지난 코드로 검증을 요청하면 만료로 처리한다. */
    private Duration accessCodeTtl = Duration.ofDays(30);

    /** 접속 코드 자릿수. */
    private int accessCodeLength = 6;
}
