package com.ditto.global.infrastructure.s3;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code app.storage.s3.*} 이미지 저장소 설정.
 * AWS 자격증명은 SDK 기본 자격증명 체인을 사용하며 이 설정에 포함하지 않는다.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {

    @NotBlank
    private String bucket;

    @NotBlank
    private String region = "ap-northeast-2";

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9/_-]*$")
    private String prefix = "images";

    private String publicBaseUrl;

    @NotNull
    private Duration presignedUrlExpiration = Duration.ofMinutes(30);

    @AssertTrue(message = "presigned URL 만료 시간은 0보다 커야 합니다.")
    public boolean isPresignedUrlExpirationValid() {
        return presignedUrlExpiration != null
                && !presignedUrlExpiration.isZero()
                && !presignedUrlExpiration.isNegative();
    }
}
