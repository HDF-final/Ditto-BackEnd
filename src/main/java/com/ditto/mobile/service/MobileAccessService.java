package com.ditto.mobile.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.course.domain.Course;
import com.ditto.course.dto.response.CourseDetailResponse;
import com.ditto.course.service.CourseService;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.mobile.config.MobileProperties;
import com.ditto.mobile.domain.AccessCode;
import com.ditto.mobile.dto.response.IssueAccessCodeResponse;
import com.ditto.mobile.dto.response.SetLocationResponse;
import com.ditto.mobile.repository.MobileAccessCodeMapper;
import com.ditto.ocr.repository.OcrPlaceMapper;

import lombok.RequiredArgsConstructor;

/**
 * 모바일 접속 흐름: 접속 코드 발급 → 검증·코스 불러오기 → 현재 위치(경로 시작점) 조회.
 *
 * <p>사장님(고객)이 코스에 대한 접속 코드를 발급하면, 로그인한 회원이 그 코드로 코스를 불러온다.
 * 접속 코드는 재배포·다중 인스턴스에도 유지되도록 Oracle 에 저장한다. 위치 조회는 서버 상태 없이
 * placeId → 길찾기 식별자만 돌려주는 무상태 연산이라 세션을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MobileAccessService {

    /** 사람이 읽고 입력하기 쉬운 문자만 사용한다(혼동되는 0·O·1·I 제외). */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_MAX_RETRY = 5;

    private final CourseService courseService;
    private final MobileAccessCodeMapper accessCodeMapper;
    private final OcrPlaceMapper ocrPlaceMapper;
    private final MobileProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 내 코스에 대한 접속 코드를 발급한다. 본인 소유 코스만 가능하다. */
    @Transactional
    public IssueAccessCodeResponse issueAccessCode(Long userId, Long courseId) {
        Course course = courseService.requireCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plus(properties.getAccessCodeTtl());
        String code = insertUniqueCode(courseId, userId, expiresAt);

        return IssueAccessCodeResponse.builder()
                .accessCode(code)
                .courseId(courseId)
                .expiresAt(expiresAt)
                .build();
    }

    /** 접속 코드를 검증해 코스 상세를 불러온다. */
    @Transactional(readOnly = true)
    public CourseDetailResponse verifyAccessCode(String rawCode) {
        String code = normalize(rawCode);
        AccessCode accessCode = accessCodeMapper.findValidByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_CODE));
        return courseService.getMobileDetail(accessCode.getCourseId());
    }

    /** 현재 위치(placeId)의 길찾기 시작점 식별자를 돌려준다. 서버 상태를 두지 않는다. */
    @Transactional(readOnly = true)
    public SetLocationResponse resolveStartPoint(Long placeId) {
        String navigationKey = ocrPlaceMapper.findNavigationKeyByPlaceId(placeId);
        if (navigationKey == null) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        return SetLocationResponse.builder()
                .placeId(placeId)
                .startNavigationKey(navigationKey)
                .build();
    }

    /** PK 충돌(중복 코드) 시 재시도하며 유일한 코드를 저장하고 그 코드를 돌려준다. */
    private String insertUniqueCode(Long courseId, Long userId, LocalDateTime expiresAt) {
        for (int i = 0; i < CODE_MAX_RETRY; i++) {
            String code = randomCode();
            try {
                accessCodeMapper.insert(code, courseId, userId, expiresAt);
                return code;
            } catch (DuplicateKeyException ignored) {
                // 코드 충돌. 다음 코드로 재시도한다.
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "접속 코드를 생성하지 못했습니다.");
    }

    /** 대소문자 구분 없이 입력받도록 대문자로 정규화한다. */
    private String normalize(String rawCode) {
        return rawCode == null ? "" : rawCode.trim().toUpperCase();
    }

    private String randomCode() {
        int length = properties.getAccessCodeLength();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }
}
