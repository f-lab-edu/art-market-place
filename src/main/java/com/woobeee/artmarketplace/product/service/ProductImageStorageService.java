package com.woobeee.artmarketplace.product.service;

import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlBatchRequest;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadBatchResponse;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadResponse;
import com.woobeee.artmarketplace.product.config.StorageProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageStorageService {
    private static final String TEMP_PRODUCT_PREFIX = "temp/products/";
    private static final String PRODUCT_PREFIX = "products/";
    private static final Map<String, String> SUPPORTED_IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public PresignedUploadResponse createPresignedUploadUrl(ProductImagePresignedUrlRequest request) {
        String contentType = normalizeContentType(request.contentType());
        String fileKey = TEMP_PRODUCT_PREFIX + UUID.randomUUID() + extensionFor(contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(fileKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageProperties.getPresignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        return new PresignedUploadResponse(
                s3Presigner.presignPutObject(presignRequest).url().toString(),
                fileKey,
                storageProperties.getPresignedUrlExpirationSeconds()
        );
    }

    public PresignedUploadBatchResponse createPresignedUploadUrls(ProductImagePresignedUrlBatchRequest request) {
        return new PresignedUploadBatchResponse(
                createPresignedUploadUrl(request.mainImage()),
                createPresignedUploadUrls(request.thumbnailImages()),
                createPresignedUploadUrls(request.detailImages())
        );
    }

    public String toProductFileKey(Long productId, String tempFileKey) {
        validateTempProductFileKey(tempFileKey);
        return PRODUCT_PREFIX + productId + "/" + tempFileKey.substring(TEMP_PRODUCT_PREFIX.length());
    }

    public void validateTempProductFileKey(String fileKey) {
        if (!StringUtils.hasText(fileKey) || !fileKey.startsWith(TEMP_PRODUCT_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product image key must be a temp product key");
        }
    }

    public void validateTempProductFileKeysExist(Collection<String> fileKeys) {
        for (String fileKey : fileKeys) {
            try {
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(storageProperties.getBucket())
                        .key(fileKey)
                        .build();

                s3Client.headObject(headObjectRequest);
            } catch (NoSuchKeyException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Uploaded product image does not exist: " + fileKey,
                        exception
                );
            }
        }
    }

    public void moveTempFilesToProduct(Map<String, String> tempToProductFileKeys) {
        List<String> copiedProductFileKeys = new ArrayList<>();

        try {
            for (Map.Entry<String, String> entry : tempToProductFileKeys.entrySet()) {
                copyObject(entry.getKey(), entry.getValue());
                copiedProductFileKeys.add(entry.getValue());
                deleteObject(entry.getKey());
            }
        } catch (RuntimeException exception) {
            deleteAllQuietly(copiedProductFileKeys);
            throw exception;
        }
    }

    public void deleteAllQuietly(Collection<String> fileKeys) {
        for (String fileKey : fileKeys) {
            try {
                deleteObject(fileKey);
            } catch (RuntimeException exception) {
                log.warn("Failed to delete product image object. key={}", fileKey, exception);
            }
        }
    }

    private void copyObject(String sourceKey, String destinationKey) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(storageProperties.getBucket())
                .sourceKey(sourceKey)
                .destinationBucket(storageProperties.getBucket())
                .destinationKey(destinationKey)
                .build();

        s3Client.copyObject(copyObjectRequest);
    }

    private void deleteObject(String fileKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(fileKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private String normalizeContentType(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private List<PresignedUploadResponse> createPresignedUploadUrls(List<ProductImagePresignedUrlRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(this::createPresignedUploadUrl)
                .toList();
    }

    private String extensionFor(String contentType) {
        String extension = SUPPORTED_IMAGE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported product image content type");
        }
        return extension;
    }
}
