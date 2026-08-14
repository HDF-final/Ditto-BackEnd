package com.ditto.global.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class S3ProviderTest {

    private static final String BUCKET = "ditto-dev-images-601202752151";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3StorageProperties properties;
    private S3Provider s3Provider;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setBucket(BUCKET);
        properties.setRegion("ap-northeast-2");
        properties.setPrefix("images");
        properties.setPublicBaseUrl("https://cdn.ditto.test/");
        properties.setPresignedUrlExpiration(Duration.ofMinutes(30));
        s3Provider = new S3Provider(s3Client, s3Presigner, properties);
    }

    @Test
    void uploadImageStoresObjectAndReturnsKeyAndUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "store.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});

        S3UploadResult result = s3Provider.uploadImage(file, "stores");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();

        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.contentLength()).isEqualTo(3L);
        assertThat(request.key()).matches(
                "images/stores/\\d{4}-\\d{2}-\\d{2}/[0-9a-f-]{36}\\.jpg");
        assertThat(result.getKey()).isEqualTo(request.key());
        assertThat(result.getUrl()).isEqualTo("https://cdn.ditto.test/" + request.key());
    }

    @Test
    void getImageUrlUsesPresignedUrlWhenPublicBaseUrlIsEmpty() throws Exception {
        properties.setPublicBaseUrl(null);
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedGetObjectRequest);
        given(presignedGetObjectRequest.url())
                .willReturn(URI.create("https://signed.ditto.test/image").toURL());
        String key = "images/stores/2026-08-14/sample.jpg";

        String result = s3Provider.getImageUrl(key);

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());
        GetObjectPresignRequest request = requestCaptor.getValue();

        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(request.getObjectRequest().bucket()).isEqualTo(BUCKET);
        assertThat(request.getObjectRequest().key()).isEqualTo(key);
        assertThat(result).isEqualTo("https://signed.ditto.test/image");
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        assertErrorCode(
                () -> s3Provider.uploadImage(file, "stores"),
                ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void uploadImageRejectsFileLargerThanTenMegabytes() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(10L * 1024 * 1024 + 1);

        assertErrorCode(
                () -> s3Provider.uploadImage(file, "stores"),
                ErrorCode.IMAGE_SIZE_EXCEEDED);
    }

    @Test
    void uploadImageRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.svg",
                "image/svg+xml",
                new byte[] {1});

        assertErrorCode(
                () -> s3Provider.uploadImage(file, "stores"),
                ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    void uploadImageRejectsUnsafeDirectory() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "store.png",
                "image/png",
                new byte[] {1});

        assertErrorCode(
                () -> s3Provider.uploadImage(file, "../stores"),
                ErrorCode.INVALID_STORAGE_PATH);
    }

    @Test
    void uploadImageConvertsSdkFailureToBusinessException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "store.webp",
                "image/webp",
                new byte[] {1});
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(S3Exception.builder().message("upload failed").statusCode(500).build());

        assertErrorCode(
                () -> s3Provider.uploadImage(file, "stores"),
                ErrorCode.S3_UPLOAD_FAILED);
    }

    @Test
    void deleteImageDeletesOnlyObjectInsideConfiguredPrefix() {
        String key = "images/users/profile/2026-08-14/sample.png";

        s3Provider.deleteImage(key);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo(key);
    }

    @Test
    void getImageUrlRejectsObjectOutsideConfiguredPrefix() {
        assertErrorCode(
                () -> s3Provider.getImageUrl("other/stores/sample.jpg"),
                ErrorCode.INVALID_STORAGE_PATH);
    }

    @Test
    void getImageUrlConvertsPresignerFailureToBusinessException() {
        properties.setPublicBaseUrl("");
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willThrow(SdkClientException.create("presign failed"));

        assertErrorCode(
                () -> s3Provider.getImageUrl("images/stores/2026-08-14/sample.jpg"),
                ErrorCode.S3_URL_GENERATION_FAILED);
    }

    @Test
    void deleteImageConvertsSdkFailureToBusinessException() {
        String key = "images/stores/2026-08-14/sample.gif";
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(S3Exception.builder().message("delete failed").statusCode(500).build());

        assertErrorCode(
                () -> s3Provider.deleteImage(key),
                ErrorCode.S3_DELETE_FAILED);
    }

    private void assertErrorCode(Runnable executable, ErrorCode expected) {
        assertThatThrownBy(executable::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
