package com.woobeee.artmarketplace.product.event;

import com.woobeee.artmarketplace.product.service.ProductImageStorageService;
import com.woobeee.artmarketplace.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageStorageEventListener {
    private final ProductImageStorageService productImageStorageService;
    private final ProductService productService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void moveTempFilesToProduct(ProductImagesRegisteredEvent event) {
        try {
            productImageStorageService.moveTempFilesToProduct(event.tempToProductFileKeys());
            productService.activateProduct(event.productId());
        } catch (RuntimeException exception) {
            log.warn("Failed to move product images. productId={}", event.productId(), exception);
            productService.markProductImageFailed(event.productId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteTempFiles(ProductImagesRegisteredEvent event) {
        productImageStorageService.deleteAllQuietly(event.tempFileKeys());
    }
}
