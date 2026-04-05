package com.woobeee.artmarketplace.auth.controller;

import com.woobeee.artmarketplace.auth.api.ApiResponse;
import com.woobeee.artmarketplace.auth.api.request.TokenIssueRequest;
import com.woobeee.artmarketplace.auth.api.request.TokenRefreshRequest;
import com.woobeee.artmarketplace.auth.api.response.TokenResponse;
import com.woobeee.artmarketplace.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/tokens")
@Tag(name = "Token Generate Controller", description = "토큰 생성 컨트롤러")
@RequiredArgsConstructor
public class TokenGenerateController {
    private final TokenService tokenService;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "refresh token 검증 후 access token, refresh token을 재발급합니다.")
    public ApiResponse<TokenResponse> refresh(
            @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpServletRequest
    ) {
        TokenResponse response = tokenService.refresh(
                request.refreshToken(),
                request.device(),
                resolveClientIp(httpServletRequest)
        );

        return ApiResponse.success(response, "Token refreshed");
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
