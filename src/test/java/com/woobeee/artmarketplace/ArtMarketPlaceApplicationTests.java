package com.woobeee.artmarketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.woobeee.artmarketplace.auth.service.AuthService;
import com.woobeee.artmarketplace.auth.service.TokenService;
import com.woobeee.artmarketplace.product.service.ProductImageStorageService;
import com.woobeee.artmarketplace.product.service.ProductService;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class ArtMarketPlaceApplicationTests {
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private ProductImageStorageService productImageStorageService;

    @MockitoBean
    private ProductService productService;

    @Test
    void contextLoads() {
    }
}
