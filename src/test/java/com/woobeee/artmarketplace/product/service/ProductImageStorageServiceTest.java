package com.woobeee.artmarketplace.product.service;

import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlBatchRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlRequest;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadBatchResponse;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadResponse;
import com.woobeee.artmarketplace.product.config.StorageProperties;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageStorageServiceTest {
    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private ProductImageStorageService productImageStorageService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setBucket("woobeee");
        storageProperties.setPresignedUrlExpirationSeconds(600);

        productImageStorageService = new ProductImageStorageService(s3Client, s3Presigner, storageProperties);
    }

    @Test
    void createPresignedUploadUrlCreatesUuidImageKeyFromContentType() throws Exception {
        when(presignedPutObjectRequest.url()).thenReturn(new URL("http://localhost:9000/woobeee/temp/products/image.jpg"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        PresignedUploadResponse response =
                productImageStorageService.createPresignedUploadUrl(new ProductImagePresignedUrlRequest(" IMAGE/JPEG "));

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        PutObjectRequest putObjectRequest = captor.getValue().putObjectRequest();

        assertThat(putObjectRequest.bucket()).isEqualTo("woobeee");
        assertThat(putObjectRequest.contentType()).isEqualTo("image/jpeg");
        assertThat(putObjectRequest.key()).startsWith("temp/products/");
        assertThat(putObjectRequest.key()).endsWith(".jpg");
        assertThat(putObjectRequest.key()).doesNotContain("/image.jpg");
        assertThat(response.fileKey()).isEqualTo(putObjectRequest.key());
        assertThat(response.expiresInSeconds()).isEqualTo(600);
    }

    @Test
    void createPresignedUploadUrlRejectsUnsupportedContentType() {
        assertThatThrownBy(() ->
                productImageStorageService.createPresignedUploadUrl(new ProductImagePresignedUrlRequest("application/pdf")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(s3Presigner);
    }

    @Test
    void createPresignedUploadUrlsCreatesGroupedUrls() throws Exception {
        when(presignedPutObjectRequest.url()).thenReturn(new URL("http://localhost:9000/woobeee/temp/products/image"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        PresignedUploadBatchResponse response = productImageStorageService.createPresignedUploadUrls(
                new ProductImagePresignedUrlBatchRequest(
                        new ProductImagePresignedUrlRequest("image/jpeg"),
                        List.of(new ProductImagePresignedUrlRequest("image/webp")),
                        List.of(new ProductImagePresignedUrlRequest("image/png"))
                )
        );

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner, org.mockito.Mockito.times(3)).presignPutObject(captor.capture());
        List<String> keys = captor.getAllValues().stream()
                .map(PutObjectPresignRequest::putObjectRequest)
                .map(PutObjectRequest::key)
                .toList();

        assertThat(keys.get(0)).endsWith(".jpg");
        assertThat(keys.get(1)).endsWith(".webp");
        assertThat(keys.get(2)).endsWith(".png");
        assertThat(response.mainImage().fileKey()).isEqualTo(keys.get(0));
        assertThat(response.thumbnailImages()).hasSize(1);
        assertThat(response.thumbnailImages().get(0).fileKey()).isEqualTo(keys.get(1));
        assertThat(response.detailImages()).hasSize(1);
        assertThat(response.detailImages().get(0).fileKey()).isEqualTo(keys.get(2));
    }

    @Test
    void validateTempProductFileKeysExistRejectsMissingObject() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        assertThatThrownBy(() -> productImageStorageService.validateTempProductFileKeysExist(
                List.of("temp/products/missing.jpg")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("Uploaded product image does not exist: temp/products/missing.jpg");
    }
}
