package com.ditto.admin.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code ditto.celeb-draft.*} — 승인 대기 코스 초안을 들고 있는 람다 접속 설정.
 *
 * <p>{@code ditto-celeb-warm-2} 는 셀럽 한 명을 조사해 <b>매장 3 + 카페 1 + 여가 1</b> 짜리
 * 코스 초안을 만들어 Redis({@code celeb:draft:*})에 하루 동안 두는 배치다. 관리자가 보고
 * 승인해야 손님에게 나가므로, 이 백엔드는 그 초안을 <b>읽기만</b> 한다.
 *
 * <p>{@code ditto.ai-engine} 과 달리 HTTP 모드가 없다. 초안 창구는 Lambda 안에서만
 * 열리는 Redis 를 보기 때문에 로컬에 같은 것을 띄울 방법이 없다. 로컬 개발자는 AWS
 * profile 로, 배포 환경은 EC2 인스턴스 역할로 자격증명을 얻는다
 * ({@code lambda:InvokeFunction} 권한이 있어야 한다).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.celeb-draft")
public class CelebDraftProperties {

    /** 호출할 함수 이름 또는 ARN */
    private String functionName = "ditto-celeb-warm-2";

    /** 함수가 있는 리전 */
    private String region = "ap-northeast-2";

    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 응답 대기 시간.
     *
     * <p>{@code ditto.ai-engine} 의 120초와 달리 짧다. 이 백엔드가 부르는 것은
     * <b>조회 창구뿐</b>이라 Redis 를 한 번 읽고 끝난다. 초안을 <i>만드는</i> 경로는
     * 인물당 2~6분이 걸리지만 여기서 부르지 않는다.
     *
     * <p>짧아야 람다가 죽었을 때 관리자 화면이 매달리지 않는다.
     */
    private Duration readTimeout = Duration.ofSeconds(10);
}
