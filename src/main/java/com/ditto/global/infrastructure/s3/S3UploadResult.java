package com.ditto.global.infrastructure.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * S3 이미지 업로드 결과.
 * 영속화할 때는 만료될 수 있는 URL이 아니라 {@code key}만 저장한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class S3UploadResult {

    private final String key;
    private final String url;
}
