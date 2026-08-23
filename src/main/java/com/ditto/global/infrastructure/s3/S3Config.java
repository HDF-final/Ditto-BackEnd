package com.ditto.global.infrastructure.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS SDK S3 클라이언트 구성.
 * 로컬 AWS profile, 환경 변수, ECS/EC2 IAM Role 순으로 기본 자격증명 체인을 사용한다.
 */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3StorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
