package com.ditto.global.infrastructure.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS SDK S3 클라이언트 구성.
 * 로컬 AWS CLI 로그인 세션, 환경 변수, ECS/EC2 IAM Role 순으로 자격증명 체인을 사용한다.
 */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3Config {

    private AwsCredentialsProvider credentialsProvider() {
        return AwsCredentialsProviderChain.builder()
                .credentialsProviders(
                        DefaultCredentialsProvider.create(),
                        ProcessCredentialsProvider.builder()
                                .command("aws configure export-credentials --format json")
                                .build()
                )
                .build();
    }

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3StorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
