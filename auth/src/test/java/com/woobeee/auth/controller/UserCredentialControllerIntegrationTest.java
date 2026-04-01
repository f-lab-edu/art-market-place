package com.woobeee.auth.controller;

import com.woobeee.auth.dto.response.AuthTokenResponse;
import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.service.OauthUserCredentialService;
import com.woobeee.auth.token.AccessTokenProvider;
import com.woobeee.auth.token.RefreshTokenCookieProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCredentialControllerImpl.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserCredentialControllerIntegrationTest.TestConfig.class)
class UserCredentialControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeOauthUserCredentialService oauthUserCredentialService;

    @BeforeEach
    void setUp() {
        oauthUserCredentialService.loginResult = issuedTokens("login-access", "login-refresh");
        oauthUserCredentialService.signInResult = issuedTokens("signin-access", "signin-refresh");
        oauthUserCredentialService.refreshResult = issuedTokens("refresh-access", "refresh-refresh");
        oauthUserCredentialService.lastLoginToken = null;
        oauthUserCredentialService.lastSignInToken = null;
        oauthUserCredentialService.lastRefreshToken = null;
        oauthUserCredentialService.lastLogoutToken = null;
    }

    @Test
    void login_returnsTokenResponseAndSetsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"google-login-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("login request success"))
                .andExpect(jsonPath("$.header.resultCode").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("login-access"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600000))
                .andExpect(cookie().value("refreshToken", "login-refresh"));

        assertThat(oauthUserCredentialService.lastLoginToken).isEqualTo("google-login-token");
    }

    @Test
    void signIn_returnsTokenResponseAndSetsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/signIn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"google-signin-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("sign in success"))
                .andExpect(jsonPath("$.data.accessToken").value("signin-access"))
                .andExpect(cookie().value("refreshToken", "signin-refresh"));

        assertThat(oauthUserCredentialService.lastSignInToken).isEqualTo("google-signin-token");
    }

    @Test
    void refresh_readsCookieDelegatesToServiceAndSetsNewCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("refresh success"))
                .andExpect(jsonPath("$.data.accessToken").value("refresh-access"))
                .andExpect(cookie().value("refreshToken", "refresh-refresh"));

        assertThat(oauthUserCredentialService.lastRefreshToken).isEqualTo("old-refresh-token");
    }

    @Test
    void logout_readsCookieClearsRefreshCookieAndReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "logout-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("logout success"))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().value("refreshToken", ""));

        assertThat(oauthUserCredentialService.lastLogoutToken).isEqualTo("logout-refresh-token");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeOauthUserCredentialService fakeOauthUserCredentialService() {
            return new FakeOauthUserCredentialService();
        }

        @Bean
        OauthUserCredentialService oauthUserCredentialService(FakeOauthUserCredentialService fakeService) {
            return fakeService;
        }

        @Bean
        RefreshTokenCookieProvider refreshTokenCookieProvider() {
            AccessTokenProvider accessTokenProvider = new AccessTokenProvider(
                    "01234567890123456789012345678901",
                    "abcdefghijklmnopqrstuvwxyz123456",
                    3_600_000L,
                    86_400_000L
            );
            return new RefreshTokenCookieProvider(
                    accessTokenProvider,
                    "refreshToken",
                    "/api/auth",
                    false,
                    "Lax"
            );
        }
    }

    static class FakeOauthUserCredentialService implements OauthUserCredentialService {
        private IssuedAuthTokens loginResult;
        private IssuedAuthTokens signInResult;
        private IssuedAuthTokens refreshResult;
        private String lastLoginToken;
        private String lastSignInToken;
        private String lastRefreshToken;
        private String lastLogoutToken;

        @Override
        public IssuedAuthTokens signIn(String idTokenString) {
            this.lastSignInToken = idTokenString;
            return signInResult;
        }

        @Override
        public IssuedAuthTokens logIn(String idTokenString) {
            this.lastLoginToken = idTokenString;
            return loginResult;
        }

        @Override
        public IssuedAuthTokens refresh(String refreshToken) {
            this.lastRefreshToken = refreshToken;
            return refreshResult;
        }

        @Override
        public void logout(String refreshToken) {
            this.lastLogoutToken = refreshToken;
        }
    }

    private static IssuedAuthTokens issuedTokens(String accessToken, String refreshToken) {
        return new IssuedAuthTokens(
                new AuthTokenResponse(accessToken, "Bearer", 3_600_000L),
                refreshToken
        );
    }
}
