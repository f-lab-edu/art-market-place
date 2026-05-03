package com.woobeee.artmarketplace.product.controller;

import com.woobeee.artmarketplace.product.api.ApiResponse;
import com.woobeee.artmarketplace.product.api.request.ProductCreateRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImageRecoveryRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlRequest;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadResponse;
import com.woobeee.artmarketplace.product.api.response.ProductCreateResponse;
import com.woobeee.artmarketplace.product.api.response.ProductImageRecoveryResponse;
import com.woobeee.artmarketplace.product.api.response.ProductListResponse;
import com.woobeee.artmarketplace.product.service.ProductImageStorageService;
import com.woobeee.artmarketplace.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Controller", description = "상품 등록 및 상품 이미지 업로드 컨트롤러")
@RequiredArgsConstructor
public class ProductController {
    private final ProductImageStorageService productImageStorageService;
    private final ProductService productService;

    @GetMapping
    @Operation(
            summary = "상품 전체 조회 / 필터 조회",
            description = "이미지 이동이 완료되어 활성화된 상품 목록을 최신순으로 조회합니다.")
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "12") Integer size
    ) {
        if (StringUtils.hasText(tag) || StringUtils.hasText(artist)) {
            ProductListResponse response = productService.getProductsByFilters(tag, artist, PageRequest.of(page, size));
            return ApiResponse.success(response, "Products retrieved");
        }
        ProductListResponse response = productService.getProducts(PageRequest.of(page, size));
        return ApiResponse.success(response, "Products retrieved");
    }

    @PostMapping("/images")
    @Operation(
            summary = "상품 이미지 Presigned URL 발급",
            description = "클라이언트가 S3/MinIO temp 경로에 직접 업로드할 PUT URL을 발급합니다.")
    public ApiResponse<PresignedUploadResponse> createImagePresignedUrl(
            @Valid @RequestBody ProductImagePresignedUrlRequest request
    ) {
        PresignedUploadResponse response = productImageStorageService.createPresignedUploadUrl(request);
        return ApiResponse.success(response, "Product image presigned URL created");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "상품 등록",
            description = "업로드 완료된 temp 이미지 key로 상품을 등록하고 커밋 후 products 경로로 이미지를 이동합니다.")
    public ApiResponse<ProductCreateResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductCreateResponse response = productService.createProduct(request);
        return ApiResponse.createSuccess(response, "Product created");
    }

    @PostMapping("/{productId}/images")
    @Operation(
            summary = "상품 이미지 수동 복구",
            description = "이미지 이동에 실패한 상품에 새 temp 이미지 key를 연결하고 커밋 후 products 경로로 이동합니다.")
    public ApiResponse<ProductImageRecoveryResponse> recoverProductImages(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageRecoveryRequest request
    ) {
        ProductImageRecoveryResponse response = productService.recoverProductImages(productId, request);
        return ApiResponse.success(response, "Product image recovery requested");
    }
}
