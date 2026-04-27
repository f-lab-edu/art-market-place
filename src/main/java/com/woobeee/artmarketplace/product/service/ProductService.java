package com.woobeee.artmarketplace.product.service;

import com.woobeee.artmarketplace.auth.repository.SellerRepository;
import com.woobeee.artmarketplace.product.api.request.ProductCreateRequest;
import com.woobeee.artmarketplace.product.api.request.ProductImageRecoveryRequest;
import com.woobeee.artmarketplace.product.api.response.ProductCreateResponse;
import com.woobeee.artmarketplace.product.api.response.ProductImageRecoveryResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductTagRepository productTagRepository;
    private final TagRepository tagRepository;
    private final ProductImageStorageService productImageStorageService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        String mainImageKey = request.mainImageKey().trim();
        List<String> detailImageKeys = normalizeList(request.detailImageKeys());
        List<String> tags = normalizeList(request.tags());
        List<String> tempFileKeys = collectAndValidateTempFileKeys(mainImageKey, detailImageKeys);

        sellerRepository.findById(request.sellerId())
                .filter(seller -> seller.isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        Product product = productRepository.saveAndFlush(Product.create(
                request.sellerId(),
                request.height().trim(),
                request.width().trim(),
                request.shape().trim(),
                request.material().trim(),
                request.price()
        ));

        Map<String, String> fileKeyMap = createProductFileKeyMap(product.getId(), tempFileKeys);

        String mainProductFileKey = fileKeyMap.get(mainImageKey);
        List<String> detailProductFileKeys = detailImageKeys.stream()
                .map(fileKeyMap::get)
                .toList();

        saveImages(product.getId(), mainProductFileKey, detailProductFileKeys);
        saveTags(product.getId(), tags);
        applicationEventPublisher.publishEvent(new ProductImagesRegisteredEvent(product.getId(), tempFileKeys, fileKeyMap));

        return new ProductCreateResponse(
                product.getId(),
                product.getSellerId(),
                product.getHeight(),
                product.getWidth(),
                product.getShape(),
                product.getMaterial(),
                tags,
                product.getPrice(),
                product.getStatus(),
                mainProductFileKey,
                detailProductFileKeys
        );
    }

    @Transactional
    public ProductImageRecoveryResponse recoverProductImages(
            Long productId,
            ProductImageRecoveryRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStatus() == ProductStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active product images cannot be recovered");
        }

        String mainImageKey = request.mainImageKey().trim();
        List<String> detailImageKeys = normalizeList(request.detailImageKeys());
        List<String> tempFileKeys = collectAndValidateTempFileKeys(mainImageKey, detailImageKeys);
        Map<String, String> fileKeyMap = createProductFileKeyMap(product.getId(), tempFileKeys);

        String mainProductFileKey = fileKeyMap.get(mainImageKey);
        List<String> detailProductFileKeys = detailImageKeys.stream()
                .map(fileKeyMap::get)
                .toList();

        productImageRepository.deleteByProductId(product.getId());
        saveImages(product.getId(), mainProductFileKey, detailProductFileKeys);
        product.markImagePending();
        applicationEventPublisher.publishEvent(new ProductImagesRegisteredEvent(product.getId(), tempFileKeys, fileKeyMap));

        return new ProductImageRecoveryResponse(
                product.getId(),
                product.getStatus(),
                mainProductFileKey,
                detailProductFileKeys
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activateProduct(Long productId) {
        productRepository.findById(productId)
                .ifPresent(Product::activate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProductImageFailed(Long productId) {
        productRepository.findById(productId)
                .ifPresent(Product::markImageFailed);
    }

    private List<String> collectAndValidateTempFileKeys(String mainImageKey, List<String> detailImageKeys) {
        productImageStorageService.validateTempProductFileKey(mainImageKey);

        LinkedHashSet<String> fileKeys = new LinkedHashSet<>();
        fileKeys.add(mainImageKey);

        for (String detailImageKey : detailImageKeys) {
            productImageStorageService.validateTempProductFileKey(detailImageKey);
            if (!fileKeys.add(detailImageKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product image keys must not be duplicated");
            }
        }

        return new ArrayList<>(fileKeys);
    }

    private Map<String, String> createProductFileKeyMap(Long productId, List<String> tempFileKeys) {
        Map<String, String> fileKeyMap = new LinkedHashMap<>();
        for (String tempFileKey : tempFileKeys) {
            fileKeyMap.put(tempFileKey, productImageStorageService.toProductFileKey(productId, tempFileKey));
        }
        return fileKeyMap;
    }

    private void saveImages(Long productId, String mainProductFileKey, List<String> detailProductFileKeys) {
        List<ProductImage> productImages = new ArrayList<>();
        productImages.add(ProductImage.create(productId, mainProductFileKey, ProductImageType.MAIN, 0));

        for (int index = 0; index < detailProductFileKeys.size(); index++) {
            productImages.add(ProductImage.create(
                    productId,
                    detailProductFileKeys.get(index),
                    ProductImageType.DETAIL,
                    index + 1
            ));
        }

        productImageRepository.saveAll(productImages);
    }

    private void saveTags(Long productId, List<String> tags) {
        if (tags.isEmpty()) {
            return;
        }

        Map<String, Tag> tagByName = findOrCreateTags(tags);

        productTagRepository.saveAll(tags.stream()
                .map(tag -> ProductTag.create(productId, tagByName.get(tag).getId()))
                .toList());
    }

    private Map<String, Tag> findOrCreateTags(List<String> tagNames) {
        Map<String, Tag> tagByName = new HashMap<>();
        tagRepository.findByNameIn(tagNames)
                .forEach(tag -> tagByName.put(tag.getName(), tag));

        List<Tag> newTags = tagNames.stream()
                .filter(tagName -> !tagByName.containsKey(tagName))
                .map(Tag::create)
                .toList();

        tagRepository.saveAll(newTags)
                .forEach(tag -> tagByName.put(tag.getName(), tag));

        return tagByName;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
