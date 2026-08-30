package com.ditto.ocr.service;

import java.util.Collections;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.ocr.client.ClovaOcrClient;
import com.ditto.ocr.client.ClovaOcrResult;
import com.ditto.ocr.config.OcrProperties;
import com.ditto.ocr.domain.OcrSession;
import com.ditto.ocr.dto.request.OcrSessionStartRequest;
import com.ditto.ocr.dto.response.OcrRecognitionResponse;
import com.ditto.ocr.dto.response.OcrSessionStartResponse;
import com.ditto.ocr.repository.OcrPlaceMapper;
import com.ditto.ocr.support.OcrImagePreprocessor;
import com.ditto.ocr.support.OcrWordPostProcessor;
import com.ditto.ocr.support.PreprocessedImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OCR 길찾기.
 *
 * <p>세션 시작은 DB(장소 식별자 조회)만 쓰고, 인식은 다음 순서로 처리한다.
 * (1) 대용량 이미지 전처리 → (2) CLOVA OCR → 층·가격·% 형태만 후처리 →
 * (3) 카탈로그 인메모리 exact/alias/fuzzy 매칭. 프로모 문구는 사전으로 지우지 않고
 * 카탈로그에 없으면 후보에서 떨어진다. {@code matchScore} 와 OCR {@code confidence} 는 분리.
 * 인식은 외부 응답을 수십 ms~수 초 기다리므로 트랜잭션으로 커넥션을 잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrNavigationService {

    private final OcrSessionStore sessionStore;
    private final OcrPlaceMapper ocrPlaceMapper;
    private final ClovaOcrClient clovaOcrClient;
    private final OcrPlaceMatcher placeMatcher;
    private final OcrImagePreprocessor imagePreprocessor;
    private final OcrWordPostProcessor wordPostProcessor;
    private final OcrProperties properties;

    /** 세션을 시작하고 시작 장소의 길찾기 식별자를 함께 돌려준다. */
    public OcrSessionStartResponse startSession(OcrSessionStartRequest request) {
        String navigationKey = ocrPlaceMapper.findNavigationKeyByPlaceId(request.getPlaceId());
        if (navigationKey == null) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }

        OcrSession session = sessionStore.create(request.getPlaceId(), request.getSource());

        return OcrSessionStartResponse.builder()
                .sessionId(session.getSessionId())
                .currentPlaceId(session.getPlaceId())
                .startNavigationKey(navigationKey)
                .build();
    }

    /** 세션을 검증한 뒤 이미지를 인식해 브랜드명과 매칭 장소 후보를 돌려준다. */
    public OcrRecognitionResponse recognize(String sessionId, MultipartFile image) {
        sessionStore.require(sessionId);
        return recognizeImage(image);
    }

    /**
     * 세션 없이 이미지만으로 현재 위치를 인식한다.
     *
     * <p>모바일 진입 시점처럼 아직 세션이 없는 단계에서 간판 이미지를 바로 인식하는 용도다.
     */
    public OcrRecognitionResponse recognizeLocation(MultipartFile image) {
        return recognizeImage(image);
    }

    /** 이미지를 인식해 브랜드명과 매칭 장소 후보를 돌려준다. */
    private OcrRecognitionResponse recognizeImage(MultipartFile image) {
        validateImage(image);

        String originalFormat = formatOf(image);
        PreprocessedImage processed = imagePreprocessor.process(readBytes(image), originalFormat);
        String fileName = processed.isTransformed() ? "ocr.jpg" : image.getOriginalFilename();

        ClovaOcrResult raw = clovaOcrClient.recognize(
                processed.getBytes(), processed.getFormat(), fileName);
        ClovaOcrResult result = wordPostProcessor.process(raw);

        String recognitionId = "ocr_" + UUID.randomUUID().toString().replace("-", "");

        if (result.isEmpty()) {
            log.debug("OCR 인식 텍스트 없음. recognitionId={}", recognitionId);
            return OcrRecognitionResponse.builder()
                    .recognitionId(recognitionId)
                    .recognizedBrandName(null)
                    .candidates(Collections.emptyList())
                    .build();
        }

        OcrPlaceMatcher.MatchResult matched =
                placeMatcher.resolve(result, properties.getMaxCandidates());

        return OcrRecognitionResponse.builder()
                .recognitionId(recognitionId)
                .recognizedBrandName(matched.getRecognizedBrandName())
                .requiresSelection(matched.isRequiresSelection())
                .candidates(matched.getCandidates())
                .build();
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (java.io.IOException e) {
            log.error("OCR 이미지 읽기 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    /** 파일명 확장자에서 포맷 힌트를 뽑는다. 알 수 없으면 jpg 로 본다. */
    private String formatOf(MultipartFile image) {
        String name = image.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot + 1).toLowerCase();
            }
        }
        return "jpg";
    }
}
