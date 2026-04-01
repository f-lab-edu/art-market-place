package com.woobeee.auth.controller;

import com.woobeee.auth.aop.Idempotent;
import com.woobeee.auth.dto.request.OauthTokenRequest;
import com.woobeee.auth.dto.response.ApiResponse;
import com.woobeee.auth.dto.response.AuthTokenResponse;
import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.service.OauthUserCredentialService;
import com.woobeee.auth.token.RefreshTokenCookieProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
public class UserCredentialControllerImpl implements UserCredentialController{
    private final OauthUserCredentialService oauthUserCredentialService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    /// GET /api/auth/login
    @Override
    public ApiResponse<AuthTokenResponse> login(OauthTokenRequest request, HttpServletResponse response) {
        log.info("login request");
        IssuedAuthTokens issuedAuthTokens = oauthUserCredentialService.logIn(request.idToken());
        refreshTokenCookieProvider.addRefreshTokenCookie(response, issuedAuthTokens.refreshToken());
        return ApiResponse.success(
                issuedAuthTokens.response(),
                "login request success"
        );
    }

    /// POST /api/auth/signIn
    @Override
    @Idempotent
    public ApiResponse<AuthTokenResponse> signIn(OauthTokenRequest request, HttpServletResponse response) {
        log.info("sign in request");
        IssuedAuthTokens issuedAuthTokens = oauthUserCredentialService.signIn(request.idToken());
        refreshTokenCookieProvider.addRefreshTokenCookie(response, issuedAuthTokens.refreshToken());
        return ApiResponse.success(
                issuedAuthTokens.response(),
                "sign in success");
    }

    @Override
    public ApiResponse<AuthTokenResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("refresh request");
        IssuedAuthTokens issuedAuthTokens = oauthUserCredentialService.refresh(refreshToken);
        refreshTokenCookieProvider.addRefreshTokenCookie(response, issuedAuthTokens.refreshToken());
        return ApiResponse.success(
                issuedAuthTokens.response(),
                "refresh success"
        );
    }

    /// GET /api/auth/logout
    @Override
    public ApiResponse<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("logout request");
        oauthUserCredentialService.logout(refreshToken);
        refreshTokenCookieProvider.clearRefreshTokenCookie(response);
        return ApiResponse.success("logout success");
    }
}
