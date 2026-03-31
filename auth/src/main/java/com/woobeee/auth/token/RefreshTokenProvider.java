package com.woobeee.auth.token;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenProvider {
    private final AccessTokenProvider accessTokenProvider;
    private final String cookieName;
    private final String cookiePath;
    private final boolean secureCookie;
    private final String sameSite;

    public RefreshTokenProvider(
            AccessTokenProvider accessTokenProvider,
            @Value("${jwt.refresh-token.cookie-name:refreshToken}") String cookieName,
            @Value("${jwt.refresh-token.cookie-path:/api/auth}") String cookiePath,
            @Value("${jwt.refresh-token.cookie-secure:false}") boolean secureCookie,
            @Value("${jwt.refresh-token.cookie-same-site:Lax}") String sameSite
    ) {
        this.accessTokenProvider = accessTokenProvider;
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(accessTokenProvider.getRefreshTokenExpirationMillis()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
