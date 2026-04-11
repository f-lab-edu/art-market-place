package com.woobeee.artmarketplace.auth.controller;

import com.woobeee.artmarketplace.auth.api.ApiResponse;
import com.woobeee.artmarketplace.auth.api.request.BuyerSignupRequest;
import com.woobeee.artmarketplace.auth.api.request.LoginRequest;
import com.woobeee.artmarketplace.auth.api.request.SellerSignupRequest;
import com.woobeee.artmarketplace.auth.api.response.TokenResponse;
import com.woobeee.artmarketplace.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Controller", description = "회원가입 및 로그인 컨트롤러")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup/buyers")
    @Operation(summary = "구매자 회원가입", description = "Google OAuth 2.0 ID 토큰으로 구매자 회원가입 후 access/refresh token을 발급합니다.")
    public ApiResponse<TokenResponse> signupBuyer(
            @Valid @RequestBody BuyerSignupRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TokenResponse response = authService.signupBuyer(request, resolveClientIp(httpServletRequest));
        return ApiResponse.success(response, "Buyer signup completed");
    }

    @PostMapping("/signup/sellers")
    @Operation(summary = "판매자 회원가입", description = "Google OAuth 2.0 ID 토큰으로 판매자 회원가입 후 access/refresh token을 발급합니다.")
    public ApiResponse<TokenResponse> signupSeller(
            @Valid @RequestBody SellerSignupRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TokenResponse response = authService.signupSeller(request, resolveClientIp(httpServletRequest));
        return ApiResponse.success(response, "Seller signup completed");
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "Google OAuth 2.0 ID 토큰과 회원 유형으로 로그인 후 access/refresh token을 발급합니다.")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TokenResponse response = authService.login(request, resolveClientIp(httpServletRequest));
        return ApiResponse.success(response, "Login completed");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
