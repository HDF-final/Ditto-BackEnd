package com.ditto.mobile.domain;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * {@code mobile_access_code} 테이블 매핑.
 *
 * <p>사장님(고객)이 코스 하나를 모바일로 열어주기 위해 발급하는 접속 코드다.
 * 코드 → 코스 매핑과 발급자를 들고 있으며, {@code expiresAt} 이 지나면 만료로 처리한다.
 */
@Getter
public class AccessCode {

    private String code;
    private Long courseId;
    private Long issuedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
