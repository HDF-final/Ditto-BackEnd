package com.ditto.global.infrastructure.s3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

/**
 * 도메인 공통 이미지 저장소.
 * 파일은 비공개 S3에 저장하고, 호출자에게 영속화용 object key와 조회 URL을 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3Provider {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_OBJECT_KEY_LENGTH = 1024;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private static final Pattern SAFE_DIRECTORY_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9/_-]*$");
    private static final Pattern SAFE_OBJECT_KEY_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._/-]+$");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /** CloudFront 에 동작이 걸린 prefix 가 아니라 버킷 직통으로 내야 하는 것. */
    private static final String DIRECT_PREFIX = "course/";
    private final S3StorageProperties properties;

    /**
     * 이미지를 S3에 업로드한다.
     *
     * @param file 업로드 이미지
     * @param directory 도메인별 저장 경로. 예: stores, users/profile
     * @return DB 저장용 key와 클라이언트 응답용 URL
     */
    public S3UploadResult uploadImage(MultipartFile file, String directory) {
        String contentType = validateAndGetContentType(file);
        String normalizedDirectory = normalizeDirectory(directory);
        String objectKey = createObjectKey(
                normalizedDirectory,
                EXTENSION_BY_CONTENT_TYPE.get(contentType));

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("S3 image uploaded. bucket={}, key={}", properties.getBucket(), objectKey);

            return S3UploadResult.builder()
                    .key(objectKey)
                    .url(getImageUrl(objectKey))
                    .build();
        } catch (IOException | SdkException exception) {
            log.error("S3 image upload failed. bucket={}, key={}",
                    properties.getBucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    /**
     * 저장된 object key로 클라이언트가 사용할 이미지 URL을 만든다.
     * CloudFront base URL이 없으면 비공개 객체용 presigned GET URL을 반환한다.
     */
    public String getImageUrl(String objectKey) {
        validateObjectKey(objectKey);

        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return removeTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.getPresignedUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException exception) {
            log.error("S3 image URL generation failed. bucket={}, key={}",
                    properties.getBucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.S3_URL_GENERATION_FAILED);
        }
    }

    /** S3에서 이미지를 삭제한다. */
    public void deleteImage(String objectKey) {
        validateObjectKey(objectKey);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
            log.info("S3 image deleted. bucket={}, key={}", properties.getBucket(), objectKey);
        } catch (SdkException exception) {
            log.error("S3 image deletion failed. bucket={}, key={}",
                    properties.getBucket(), objectKey, exception);
            throw new BusinessException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    private String validateAndGetContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(normalizedContentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        return normalizedContentType;
    }

    private String normalizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_PATH);
        }

        String normalized = directory.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "")
                .toLowerCase(Locale.ROOT);

        if (normalized.contains("..")
                || normalized.contains("//")
                || normalized.contains("\\")
                || !SAFE_DIRECTORY_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_PATH);
        }
        return normalized;
    }

    private String createObjectKey(String directory, String extension) {
        return String.join(
                "/",
                normalizeDirectory(properties.getPrefix()),
                directory,
                LocalDate.now(ZoneOffset.UTC).toString(),
                UUID.randomUUID() + "." + extension);
    }

    /**
     * 앱이 업로드하지 않고 이미 S3에 존재하는 object key(예: {@code place-picture/*})로 조회 URL을 만든다.
     * 업로드 경로와 달리 {@code images/} prefix 제약과 ASCII 제한을 두지 않으므로 한글·공백이 든
     * 파일명도 허용하되, 경로 이탈 문자는 막는다. key가 비어 있으면 {@code null}을 반환한다.
     *
     * @param objectKey S3 object key (null/빈 문자열 허용)
     * @return 조회용 URL, key가 없으면 null
     */
    public String resolveImageUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        validateReadableKey(objectKey);

        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return removeTrailingSlash(properties.getPublicBaseUrl()) + "/" + objectKey;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.getPresignedUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException exception) {
            // 이미지 URL 하나가 실패해도 목록/상세 응답 전체가 죽지 않도록 null을 반환한다.
            log.error("S3 image URL generation failed. bucket={}, key={}",
                    properties.getBucket(), objectKey, exception);
            return null;
        }
    }

    /**
     * <b>key 의 prefix 를 보고 CDN 과 직통 중에 고른다.</b> 코스 사진처럼 두 갈래가
     * 섞여 오는 자리에서 쓴다.
     *
     * <p>반영 람다가 자리 사진을 두 군데에 둔다 — 기사에서 받아 온 셀럽 사진은
     * {@code course/{share_code}/n.jpg} 로 새로 올리고, 매장 사진은 이미 버킷에 있는
     * {@code place-picture/…} 를 그대로 가리킨다. 앞의 것은 CloudFront 에 동작이
     * 없어 {@link #resolveImageUrl} 로 보내면 301 로 튕긴다.
     *
     * <p>{@code RecommendedCourseService.heroUrl} 이 쓰던 규칙을 여기로 올린 것이다.
     * 코스 상세도 같은 판단을 해야 하는데, 같은 규칙이 두 군데에 적혀 있으면 한쪽만
     * 고쳐지는 날이 온다. <b>대표 사진과 자리 사진이 어긋나면 안 된다.</b>
     *
     * @param objectKey S3 object key (null/빈 문자열 허용)
     * @return 조회용 URL, key가 없으면 null
     */
    public String resolveImageUrlByPrefix(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return objectKey.startsWith(DIRECT_PREFIX)
                ? resolveDirectImageUrl(objectKey)
                : resolveImageUrl(objectKey);
    }

    /**
     * <b>CDN 을 건너뛴 버킷 직통 주소.</b> CloudFront 가 안 내주는 prefix 전용이다.
     *
     * <p>{@link #resolveImageUrl}이 {@code public-base-url}(CloudFront)을 붙이는데, 그 배포에는
     * 동작(behavior)이 {@code brand-logo/*} · {@code place-picture/*} · {@code products/*} ·
     * {@code course-resource/*} 넷만 걸려 있다. 나머지는 <b>기본 동작이 ALB</b> 라 이미지 요청이
     * 301 로 튕긴다 — 실측이다.
     *
     * <p>그래서 그 밖의 prefix 는 여기로 온다. 버킷 객체가 공개 읽기라 서명이 필요 없고,
     * 주소가 고정이라 브라우저가 캐시한다(프리사인은 부를 때마다 주소가 바뀐다).
     *
     * <p><b>CloudFront 에 그 prefix 동작이 생기면 이 메서드를 쓸 이유가 없어진다.</b> 그때는
     * 호출부를 {@link #resolveImageUrl} 로 되돌리면 된다.
     *
     * @param objectKey S3 object key (null/빈 문자열 허용)
     * @return 조회용 URL, key가 없으면 null
     */
    public String resolveDirectImageUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        validateReadableKey(objectKey);
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.getBucket(), properties.getRegion(), encodeKeyPath(objectKey));
    }

    /** 경로 조각마다 인코딩한다. 슬래시는 살려야 키가 안 깨진다. */
    private static String encodeKeyPath(String objectKey) {
        String[] segments = objectKey.split("/", -1);
        StringBuilder out = new StringBuilder(objectKey.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            // URLEncoder 는 폼 인코딩이라 공백을 '+' 로 낸다. 경로에서는 %20 이어야 한다.
            out.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return out.toString();
    }

    /** 읽기용 key 검증. prefix·문자셋은 제한하지 않고 경로 이탈만 차단한다. */
    private void validateReadableKey(String objectKey) {
        if (objectKey.length() > MAX_OBJECT_KEY_LENGTH
                || objectKey.startsWith("/")
                || objectKey.contains("..")
                || objectKey.contains("//")
                || objectKey.contains("\\")) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_PATH);
        }
    }

    private void validateObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)
                || objectKey.length() > MAX_OBJECT_KEY_LENGTH
                || objectKey.startsWith("/")
                || objectKey.contains("..")
                || objectKey.contains("//")
                || objectKey.contains("\\")
                || !SAFE_OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_PATH);
        }

        String rootPrefix = normalizeDirectory(properties.getPrefix());
        if (!objectKey.startsWith(rootPrefix + "/")) {
            throw new BusinessException(ErrorCode.INVALID_STORAGE_PATH);
        }
    }

    private String removeTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}
