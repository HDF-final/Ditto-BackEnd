package com.ditto.ocr.support;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OCR 인식 엔드포인트에 IP 단위 레이트 리밋을 적용하는 인터셉터.
 *
 * <p>상한을 넘으면 {@link ErrorCode#OCR_RATE_LIMITED}(429)를 던져 컨트롤러 진입 전에 막는다.
 * 예외는 전역 예외 처리기가 표준 에러 응답으로 변환한다.
 *
 * <p>스프링 빈으로 자동 감지({@code @Component})하지 않고 {@link OcrWebConfig} 가 직접
 * 생성해 등록한다. {@code HandlerInterceptor} 빈은 {@code @WebMvcTest} 슬라이스가 모두
 * 로드하는데, 이 인터셉터를 빈으로 두면 협력 빈 {@link OcrRateLimiter} 가 없는 다른 컨트롤러
 * 슬라이스 테스트까지 컨텍스트 로딩이 깨지기 때문이다.
 */
@RequiredArgsConstructor
public class OcrRateLimitInterceptor implements HandlerInterceptor {

    private final OcrRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimiter.tryAcquire(clientIp(request))) {
            throw new BusinessException(ErrorCode.OCR_RATE_LIMITED);
        }
        return true;
    }

    /** 프록시 뒤라면 X-Forwarded-For 의 첫 홉을, 아니면 원격 주소를 클라이언트 키로 쓴다. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
