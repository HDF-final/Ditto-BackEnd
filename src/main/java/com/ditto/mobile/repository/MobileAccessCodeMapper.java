package com.ditto.mobile.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.mobile.domain.AccessCode;

/**
 * mobile_access_code 테이블 매퍼. SQL 은 resources/mapper/MobileAccessCodeMapper.xml 에 있다.
 */
@Mapper
public interface MobileAccessCodeMapper {

    /** 접속 코드를 저장한다. code 는 PK 라 중복 시 예외가 발생한다(호출측에서 재시도). */
    int insert(@Param("code") String code,
               @Param("courseId") Long courseId,
               @Param("issuedByUserId") Long issuedByUserId,
               @Param("expiresAt") LocalDateTime expiresAt);

    /** 만료되지 않은 유효한 접속 코드를 조회한다. */
    Optional<AccessCode> findValidByCode(@Param("code") String code);

    /** 만료된 접속 코드를 일괄 삭제한다. */
    int deleteExpired();
}
