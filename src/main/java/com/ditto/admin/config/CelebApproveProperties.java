package com.ditto.admin.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code ditto.celeb-approve.*} — 승인 람다 접속 설정.
 *
 * <p>{@code ditto.celeb-draft} 와 <b>따로 둔다.</b> 저쪽은 Redis 를 한 번 읽고 끝나
 * 10초에 끊지만, 이쪽은 Redis 네 번에 Oracle MERGE 와 만료 행 청소까지 있다.
 * 빈 하나를 나눠 쓰면 둘 중 하나가 남의 정책을 쓰게 된다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.celeb-approve")
public class CelebApproveProperties {

    /** 호출할 함수 이름 또는 ARN */
    private String functionName = "ditto-celeb-approve";

    /** 함수가 있는 리전 */
    private String region = "ap-northeast-2";

    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 응답 대기 시간.
     *
     * <p>조회 창구(10초)보다 길다. Oracle MERGE 와 만료 행 청소가 붙고, VPC 안의
     * 람다라 콜드스타트도 있다. 그래도 60초를 넘기지 않는다 — 관리자가 그보다 오래
     * 기다리면 화면이 매달린 것과 구별이 안 된다.
     */
    private Duration readTimeout = Duration.ofSeconds(30);
}
