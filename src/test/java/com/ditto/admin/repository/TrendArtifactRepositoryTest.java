package com.ditto.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.admin.domain.TrendArtifactType;
import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class TrendArtifactRepositoryTest {

    @Mock
    private S3Client s3Client;

    private TrendArtifactRepository repository;

    @BeforeEach
    void setUp() {
        S3StorageProperties properties = new S3StorageProperties();
        properties.setBucket("hdf-ditto-images");
        repository = new TrendArtifactRepository(s3Client, properties, new ObjectMapper());
    }

    @Test
    void readsOnlyConfiguredArtifactAndReturnsMetadata() {
        String json = """
                {
                  "status": "partial",
                  "builtAt": "2026-08-24T06:48:30Z",
                  "countries": {"KR": []},
                  "warnings": ["one", "two"]
                }
                """;
        GetObjectResponse objectResponse = GetObjectResponse.builder()
                .lastModified(Instant.parse("2026-08-24T06:48:31Z"))
                .eTag("etag-1")
                .build();
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willAnswer(invocation -> {
                    GetObjectRequest request = invocation.getArgument(0);
                    assertThat(request.bucket()).isEqualTo("hdf-ditto-images");
                    assertThat(request.key()).isEqualTo("trends/country-ranking/latest-top10.json");
                    return ResponseBytes.fromByteArray(
                            objectResponse,
                            json.getBytes(StandardCharsets.UTF_8));
                });

        TrendArtifactResponse response = repository.findLatest(TrendArtifactType.TOP10);

        assertThat(response.getArtifact()).isEqualTo("top10");
        assertThat(response.getStatus()).isEqualTo("partial");
        assertThat(response.getBuiltAt()).isEqualTo("2026-08-24T06:48:30Z");
        assertThat(response.getWarningCount()).isEqualTo(2);
        assertThat(response.getLastModified()).isEqualTo(Instant.parse("2026-08-24T06:48:31Z"));
        assertThat(response.getPayload().path("countries").has("KR")).isTrue();
    }

    @Test
    void mapsMissingObjectToNotFoundError() {
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willThrow(S3Exception.builder().statusCode(404).message("not found").build());

        assertThatThrownBy(() -> repository.findLatest(TrendArtifactType.CANDIDATES))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TREND_ARTIFACT_NOT_FOUND));
    }

    @Test
    void mapsMalformedJsonToReadFailure() {
        GetObjectResponse objectResponse = GetObjectResponse.builder().build();
        given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .willReturn(ResponseBytes.fromByteArray(
                        objectResponse,
                        "not-json".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> repository.findLatest(TrendArtifactType.YOUTUBE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TREND_ARTIFACT_READ_FAILED));
    }
}
