package com.ditto.admin.repository;

import java.io.IOException;

import org.springframework.stereotype.Repository;

import com.ditto.admin.domain.TrendArtifactType;
import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3StorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** S3에 저장된 Lambda 트렌드 산출물을 읽는 관리자 전용 저장소. */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TrendArtifactRepository {

    private final S3Client s3Client;
    private final S3StorageProperties properties;
    private final ObjectMapper objectMapper;

    public TrendArtifactResponse findLatest(TrendArtifactType type) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(type.getObjectKey())
                .build();

        try {
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            GetObjectResponse objectResponse = responseBytes.response();
            JsonNode payload = objectMapper.readTree(responseBytes.asByteArray());

            return TrendArtifactResponse.builder()
                    .artifact(type.getCode())
                    .displayName(type.getDisplayName())
                    .objectKey(type.getObjectKey())
                    .lastModified(objectResponse.lastModified())
                    .eTag(objectResponse.eTag())
                    .status(textOrNull(payload, "status"))
                    .builtAt(textOrNull(payload, "builtAt"))
                    .warningCount(warningCount(payload))
                    .payload(payload)
                    .build();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException(ErrorCode.TREND_ARTIFACT_NOT_FOUND);
            }
            log.error("S3 trend artifact read failed. bucket={}, key={}",
                    properties.getBucket(), type.getObjectKey(), exception);
            throw new BusinessException(ErrorCode.TREND_ARTIFACT_READ_FAILED);
        } catch (IOException | SdkException exception) {
            log.error("Trend artifact parsing/read failed. bucket={}, key={}",
                    properties.getBucket(), type.getObjectKey(), exception);
            throw new BusinessException(ErrorCode.TREND_ARTIFACT_READ_FAILED);
        }
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private int warningCount(JsonNode payload) {
        JsonNode warnings = payload.get("warnings");
        return warnings != null && warnings.isArray() ? warnings.size() : 0;
    }
}
