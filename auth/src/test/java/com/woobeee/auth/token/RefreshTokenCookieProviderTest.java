package com.woobeee.auth.token;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieProviderTest {
    @Test
    void addRefreshTokenCookie_setsConfiguredCookieAttributes() {
        AccessTokenProvider accessTokenProvider = new AccessTokenProvider(
                "01234567890123456789012345678901",
                "abcdefghijklmnopqrstuvwxyz123456",
                3_600_000L,
                86_400_000L
        );
        RefreshTokenCookieProvider refreshTokenProvider = new RefreshTokenCookieProvider(
                accessTokenProvider,
                "refreshToken",
                "/api/auth",
                true,
                "Strict"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenProvider.addRefreshTokenCookie(response, "refresh-token-value");

        String setCookie = response.getHeader("Set-Cookie");

        assertThat(setCookie).contains("refreshToken=refresh-token-value");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Path=/api/auth");
        assertThat(setCookie).contains("Max-Age=86400");
    }

    @Test
    void clearRefreshTokenCookie_expiresCookieImmediately() {
        AccessTokenProvider accessTokenProvider = new AccessTokenProvider(
                "01234567890123456789012345678901",
                "abcdefghijklmnopqrstuvwxyz123456",
                3_600_000L,
                86_400_000L
        );
        RefreshTokenCookieProvider refreshTokenProvider = new RefreshTokenCookieProvider(
                accessTokenProvider,
                "refreshToken",
                "/api/auth",
                false,
                "Lax"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenProvider.clearRefreshTokenCookie(response);

        String setCookie = response.getHeader("Set-Cookie");

        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/api/auth");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).doesNotContain("Secure");
    }
}
