package com.woobeee.artmarketplace.product.service;

import com.woobeee.artmarketplace.auth.entity.Seller;
import com.woobeee.artmarketplace.auth.repository.SellerRepository;
import com.woobeee.artmarketplace.product.api.request.ProductCreateRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImageRecoveryRequest;
import com.woobeee.artmarketplace.product.api.response.ProductCreateResponse;
import com.woobeee.artmarketplace.product.api.response.ProductImageRecoveryResponse;
import com.woobeee.artmarketplace.product.api.response.ProductListResponse;
import com.woobeee.artmarketplace.product.entity.Product;
import com.woobeee.artmarketplace.product.entity.ProductImage;
import com.woobeee.artmarketplace.product.entity.ProductImageType;
import com.woobeee.artmarketplace.product.entity.ProductStatus;
import com.woobeee.artmarketplace.product.entity.ProductTag;
import com.woobeee.artmarketplace.product.entity.Tag;
import com.woobeee.artmarketplace.product.event.ProductImagesRegisteredEvent;
import com.woobeee.artmarketplace.product.repository.ProductImageRepository;
import com.woobeee.artmarketplace.product.repository.ProductRepository;
import com.woobeee.artmarketplace.product.repository.ProductTagRepository;
import com.woobeee.artmarketplace.product.repository.TagRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ProductImageStorageService productImageStorageService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSavesProductImagesTagsAndPublishesStorageEvent() {
        ProductCreateRequest request = new ProductCreateRequest(
                7L,
                " 120cm ",
                " 80cm ",
                " rectangle ",
                " canvas ",
                List.of(" oil ", "modern", "oil"),
                new BigDecimal("150000.00"),
                "temp/products/uuid/main.jpg",
                List.of("temp/products/uuid/thumb.jpg"),
                List.of("temp/products/uuid/detail.jpg")
        );
        Seller seller = Seller.create("google-sub", "seller@example.com", "seller", true, true, null);
        when(sellerRepository.findById(7L)).thenReturn(Optional.of(seller));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 99L);
            return product;
        });
        when(productImageStorageService.toProductFileKey(99L, "temp/products/uuid/main.jpg"))
                .thenReturn("products/99/uuid/main.jpg");
        when(productImageStorageService.toProductFileKey(99L, "temp/products/uuid/thumb.jpg"))
                .thenReturn("products/99/uuid/thumb.jpg");
        when(productImageStorageService.toProductFileKey(99L, "temp/products/uuid/detail.jpg"))
                .thenReturn("products/99/uuid/detail.jpg");
        when(tagRepository.findByNameIn(anyCollection()))
                .thenReturn(List.of(tagWithId("oil", 11L)));
        when(tagRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            List<Tag> tags = new ArrayList<>(invocation.getArgument(0));
            for (int index = 0; index < tags.size(); index++) {
                ReflectionTestUtils.setField(tags.get(index), "id", 20L + index);
            }
            return tags;
        });

        ProductCreateResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getSellerId()).isEqualTo(7L);
        assertThat(savedProduct.getHeight()).isEqualTo("120cm");
        assertThat(savedProduct.getWidth()).isEqualTo("80cm");
        assertThat(savedProduct.getShape()).isEqualTo("rectangle");
        assertThat(savedProduct.getMaterial()).isEqualTo("canvas");
        assertThat(savedProduct.isActive()).isFalse();
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.IMAGE_PENDING);

        ArgumentCaptor<Iterable<ProductImage>> imageCaptor = iterableCaptor();
        verify(productImageRepository).saveAll(imageCaptor.capture());
        List<ProductImage> images = toList(imageCaptor.getValue());
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getProductId()).isEqualTo(99L);
        assertThat(images.get(0).getFileKey()).isEqualTo("products/99/uuid/main.jpg");
        assertThat(images.get(0).getType()).isEqualTo(ProductImageType.MAIN);
        assertThat(images.get(1).getFileKey()).isEqualTo("products/99/uuid/thumb.jpg");
        assertThat(images.get(1).getType()).isEqualTo(ProductImageType.THUMBNAIL);
        assertThat(images.get(2).getFileKey()).isEqualTo("products/99/uuid/detail.jpg");
        assertThat(images.get(2).getType()).isEqualTo(ProductImageType.DETAIL);

        ArgumentCaptor<Iterable<ProductTag>> productTagCaptor = iterableCaptor();
        verify(productTagRepository).saveAll(productTagCaptor.capture());
        List<ProductTag> productTags = toList(productTagCaptor.getValue());
        assertThat(productTags).extracting(ProductTag::getProductId).containsOnly(99L);
        assertThat(productTags).extracting(ProductTag::getTagId).containsExactly(11L, 20L);

        ArgumentCaptor<ProductImagesRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(ProductImagesRegisteredEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        ProductImagesRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.productId()).isEqualTo(99L);
        assertThat(event.tempFileKeys()).containsExactly(
                "temp/products/uuid/main.jpg",
                "temp/products/uuid/thumb.jpg",
                "temp/products/uuid/detail.jpg"
        );
        assertThat(event.tempToProductFileKeys())
                .containsEntry("temp/products/uuid/main.jpg", "products/99/uuid/main.jpg")
                .containsEntry("temp/products/uuid/thumb.jpg", "products/99/uuid/thumb.jpg")
                .containsEntry("temp/products/uuid/detail.jpg", "products/99/uuid/detail.jpg");

        assertThat(response.productId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(ProductStatus.IMAGE_PENDING);
        assertThat(response.tags()).containsExactly("oil", "modern");
        assertThat(response.mainImageKey()).isEqualTo("products/99/uuid/main.jpg");
        assertThat(response.thumbnailImageKeys()).containsExactly("products/99/uuid/thumb.jpg");
        assertThat(response.detailImageKeys()).containsExactly("products/99/uuid/detail.jpg");
    }

    @Test
    void createProductFailsWhenSellerDoesNotExist() {
        ProductCreateRequest request = createSimpleRequest();
        when(sellerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Seller not found");

        verifyNoInteractions(productRepository, productImageRepository, productTagRepository, tagRepository);
    }

    @Test
    void createProductFailsBeforeSavingWhenUploadedImageDoesNotExist() {
        ProductCreateRequest request = createSimpleRequest();
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded product image does not exist");
        doThrow(exception)
                .when(productImageStorageService)
                .validateTempProductFileKeysExist(List.of("temp/products/uuid/main.jpg"));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isSameAs(exception);

        verifyNoInteractions(sellerRepository, productRepository, productImageRepository, productTagRepository, tagRepository);
    }

    @Test
    void recoverProductImagesReplacesImagesAndPublishesStorageEvent() {
        Product product = productWithId(99L);
        product.markImageFailed();
        ProductImageRecoveryRequest request = new ProductImageRecoveryRequest(
                "temp/products/recovery/main.jpg",
                List.of("temp/products/recovery/thumb.jpg"),
                List.of("temp/products/recovery/detail.jpg")
        );
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));
        when(productImageStorageService.toProductFileKey(99L, "temp/products/recovery/main.jpg"))
                .thenReturn("products/99/recovery/main.jpg");
        when(productImageStorageService.toProductFileKey(99L, "temp/products/recovery/thumb.jpg"))
                .thenReturn("products/99/recovery/thumb.jpg");
        when(productImageStorageService.toProductFileKey(99L, "temp/products/recovery/detail.jpg"))
                .thenReturn("products/99/recovery/detail.jpg");

        ProductImageRecoveryResponse response = productService.recoverProductImages(99L, request);

        verify(productImageRepository).deleteByProductId(99L);

        ArgumentCaptor<Iterable<ProductImage>> imageCaptor = iterableCaptor();
        verify(productImageRepository).saveAll(imageCaptor.capture());
        List<ProductImage> images = toList(imageCaptor.getValue());
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getFileKey()).isEqualTo("products/99/recovery/main.jpg");
        assertThat(images.get(1).getFileKey()).isEqualTo("products/99/recovery/thumb.jpg");
        assertThat(images.get(1).getType()).isEqualTo(ProductImageType.THUMBNAIL);
        assertThat(images.get(2).getFileKey()).isEqualTo("products/99/recovery/detail.jpg");

        ArgumentCaptor<ProductImagesRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(ProductImagesRegisteredEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().tempFileKeys()).containsExactly(
                "temp/products/recovery/main.jpg",
                "temp/products/recovery/thumb.jpg",
                "temp/products/recovery/detail.jpg"
        );

        assertThat(product.isActive()).isFalse();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.IMAGE_PENDING);
        assertThat(response.productId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(ProductStatus.IMAGE_PENDING);
        assertThat(response.mainImageKey()).isEqualTo("products/99/recovery/main.jpg");
        assertThat(response.thumbnailImageKeys()).containsExactly("products/99/recovery/thumb.jpg");
    }

    @Test
    void recoverProductImagesRejectsActiveProduct() {
        Product product = productWithId(99L);
        product.activate();
        ProductImageRecoveryRequest request = new ProductImageRecoveryRequest(
                "temp/products/recovery/main.jpg",
                List.of(),
                List.of()
        );
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.recoverProductImages(99L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("Active product images cannot be recovered");

        verifyNoInteractions(productImageRepository, applicationEventPublisher);
    }

    @Test
    void getProductsReturnsActiveProductSummaries() {
        Product product = productWithId(99L);
        product.activate();
        Seller seller = sellerWithId(7L, "artist");
        PageRequest pageable = PageRequest.of(0, 12);
        when(productRepository.findByActiveTrueOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(sellerRepository.findAllById(any())).thenReturn(List.of(seller));
        when(productImageRepository.findByProductIdInOrderByProductIdAscSortOrderAsc(List.of(99L)))
                .thenReturn(List.of(
                        image(99L, "products/99/main.jpg", ProductImageType.MAIN, 0),
                        image(99L, "products/99/thumb.jpg", ProductImageType.THUMBNAIL, 1),
                        image(99L, "products/99/detail.jpg", ProductImageType.DETAIL, 2)
                ));
        when(productTagRepository.findByProductIdIn(List.of(99L)))
                .thenReturn(List.of(ProductTag.create(99L, 11L)));
        when(tagRepository.findAllById(any()))
                .thenReturn(List.of(tagWithId("oil", 11L)));

        ProductListResponse response = productService.getProducts(pageable);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.contents()).hasSize(1);
        ProductListResponse.ProductSummary summary = response.contents().get(0);
        assertThat(summary.productId()).isEqualTo(99L);
        assertThat(summary.sellerId()).isEqualTo(7L);
        assertThat(summary.artist()).isEqualTo("artist");
        assertThat(summary.tags()).containsExactly("oil");
        assertThat(summary.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(summary.mainImageKey()).isEqualTo("products/99/main.jpg");
        assertThat(summary.thumbnailImageKeys()).containsExactly("products/99/thumb.jpg");
        assertThat(summary.detailImageKeys()).containsExactly("products/99/detail.jpg");
    }

    @Test
    void getProductsByFiltersTrimsTagAndArtist() {
        PageRequest pageable = PageRequest.of(0, 12);
        when(productRepository.findActiveProductsByFilters("oil", "artist", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        ProductListResponse response = productService.getProductsByFilters(" oil ", " artist ", pageable);

        assertThat(response.contents()).isEmpty();
        verify(productRepository).findActiveProductsByFilters("oil", "artist", pageable);
    }

    @Test
    void markProductImageFailedChangesProductStatus() {
        Product product = productWithId(99L);
        product.markImagePending();
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        productService.markProductImageFailed(99L);

        verify(productImageRepository).deleteByProductId(99L);
        assertThat(product.isActive()).isFalse();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.IMAGE_FAILED);
    }

    @Test
    void activateProductChangesProductStatus() {
        Product product = productWithId(99L);
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        productService.activateProduct(99L);

        assertThat(product.isActive()).isTrue();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    private ProductCreateRequest createSimpleRequest() {
        return new ProductCreateRequest(
                7L,
                "120cm",
                "80cm",
                "rectangle",
                "canvas",
                List.of("oil"),
                new BigDecimal("150000.00"),
                "temp/products/uuid/main.jpg",
                List.of(),
                List.of()
        );
    }

    private Product productWithId(Long id) {
        Product product = Product.create(
                7L,
                "120cm",
                "80cm",
                "rectangle",
                "canvas",
                new BigDecimal("150000.00")
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Seller sellerWithId(Long id, String nickname) {
        Seller seller = Seller.create("google-sub", "seller@example.com", nickname, true, true, null);
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }

    private ProductImage image(Long productId, String fileKey, ProductImageType type, int sortOrder) {
        return ProductImage.create(productId, fileKey, type, sortOrder);
    }

    private Tag tagWithId(String name, Long id) {
        Tag tag = Tag.create(name);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }
}
