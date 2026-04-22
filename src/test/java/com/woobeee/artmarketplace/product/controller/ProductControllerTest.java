package com.woobeee.artmarketplace.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woobeee.artmarketplace.product.api.request.ProductCreateRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImagePresignedUrlRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImageRecoveryRequest;
import com.woobeee.artmarketplace.product.api.response.PresignedUploadResponse;
import com.woobeee.artmarketplace.product.api.response.ProductCreateResponse;
import com.woobeee.artmarketplace.product.api.response.ProductImageRecoveryResponse;
import com.woobeee.artmarketplace.product.entity.ProductStatus;
import com.woobeee.artmarketplace.product.exception.ProductRestControllerAdvice;
import com.woobeee.artmarketplace.product.service.ProductImageStorageService;
import com.woobeee.artmarketplace.product.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductRestControllerAdvice.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductImageStorageService productImageStorageService;

    @MockitoBean
    private ProductService productService;

    @Test
    void createImagePresignedUrlReturnsUploadResponse() throws Exception {
        ProductImagePresignedUrlRequest request =
                new ProductImagePresignedUrlRequest("main.jpg", "image/jpeg");
        PresignedUploadResponse response = new PresignedUploadResponse(
                "http://localhost:9000/woobeee/temp/products/uuid/main.jpg",
                "temp/products/uuid/main.jpg",
                600
        );
        when(productImageStorageService.createPresignedUploadUrl(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/products/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("Product image presigned URL created"))
                .andExpect(jsonPath("$.data.uploadUrl")
                        .value("http://localhost:9000/woobeee/temp/products/uuid/main.jpg"))
                .andExpect(jsonPath("$.data.fileKey").value("temp/products/uuid/main.jpg"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(600));

        verify(productImageStorageService).createPresignedUploadUrl(request);
    }

    @Test
    void createProductReturnsProductCreateResponse() throws Exception {
        ProductCreateRequest request = createProductRequest();
        ProductCreateResponse response = new ProductCreateResponse(
                99L,
                7L,
                "120cm",
                "80cm",
                "rectangle",
                "canvas",
                List.of("oil", "modern"),
                new BigDecimal("150000.00"),
                ProductStatus.IMAGE_PENDING,
                "products/99/uuid/main.jpg",
                List.of("products/99/uuid/detail.jpg")
        );
        when(productService.createProduct(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("Product created"))
                .andExpect(jsonPath("$.data.productId").value(99))
                .andExpect(jsonPath("$.data.sellerId").value(7))
                .andExpect(jsonPath("$.data.status").value("IMAGE_PENDING"))
                .andExpect(jsonPath("$.data.mainImageKey").value("products/99/uuid/main.jpg"))
                .andExpect(jsonPath("$.data.detailImageKeys[0]").value("products/99/uuid/detail.jpg"));

        verify(productService).createProduct(request);
    }

    @Test
    void recoverProductImagesReturnsRecoveryResponse() throws Exception {
        ProductImageRecoveryRequest request = new ProductImageRecoveryRequest(
                "temp/products/recovery/main.jpg",
                List.of("temp/products/recovery/detail.jpg")
        );
        ProductImageRecoveryResponse response = new ProductImageRecoveryResponse(
                99L,
                ProductStatus.IMAGE_PENDING,
                "products/99/recovery/main.jpg",
                List.of("products/99/recovery/detail.jpg")
        );
        when(productService.recoverProductImages(eq(99L), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/products/{productId}/images", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("Product image recovery requested"))
                .andExpect(jsonPath("$.data.productId").value(99))
                .andExpect(jsonPath("$.data.status").value("IMAGE_PENDING"))
                .andExpect(jsonPath("$.data.mainImageKey").value("products/99/recovery/main.jpg"))
                .andExpect(jsonPath("$.data.detailImageKeys[0]").value("products/99/recovery/detail.jpg"));

        verify(productService).recoverProductImages(99L, request);
    }

    @Test
    void createProductRejectsInvalidRequestBody() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                7L,
                " ",
                "80cm",
                "rectangle",
                "canvas",
                List.of("oil"),
                new BigDecimal("150000.00"),
                "temp/products/uuid/main.jpg",
                List.of("temp/products/uuid/detail.jpg")
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.header.isSuccessful").value(false))
                .andExpect(jsonPath("$.header.message").value("Height is required"));

        verifyNoInteractions(productService, productImageStorageService);
    }

    private ProductCreateRequest createProductRequest() {
        return new ProductCreateRequest(
                7L,
                "120cm",
                "80cm",
                "rectangle",
                "canvas",
                List.of("oil", "modern"),
                new BigDecimal("150000.00"),
                "temp/products/uuid/main.jpg",
                List.of("temp/products/uuid/detail.jpg")
        );
    }
}
