package com.ditto.mobile.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.mobile.repository.MobileAccessCodeMapper;

import lombok.RequiredArgsConstructor;

/**
 * 만료된 접속 코드 주기적 청소.
 *
 * <p>조회 시점에도 만료를 걸러내지만(지연 만료), 검증되지 않고 버려진 코드가 쌓이는 것을
 * 막기 위해 주기적으로 만료 행을 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class MobileExpiryCleaner {

    private final MobileAccessCodeMapper accessCodeMapper;

    @Scheduled(fixedDelay = 600_000L)
    @Transactional
    public void evictExpired() {
        accessCodeMapper.deleteExpired();
    }
}
