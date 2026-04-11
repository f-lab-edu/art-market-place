package com.woobeee.artmarketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.woobeee.artmarketplace.auth.service.AuthService;
import com.woobeee.artmarketplace.auth.service.TokenService;

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

    @Test
    void contextLoads() {
    }
}
