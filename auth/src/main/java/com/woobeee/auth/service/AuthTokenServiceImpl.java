package com.woobeee.auth.service;

import com.woobeee.auth.dto.response.AuthTokenResponse;
import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.entity.Auth;
import com.woobeee.auth.entity.UserAuth;
import com.woobeee.auth.entity.UserCredential;
import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.ErrorCode;
import com.woobeee.auth.exception.JwtExpiredException;
import com.woobeee.auth.exception.JwtNotValidException;
import com.woobeee.auth.exception.UserNotFoundException;
import com.woobeee.auth.repository.AuthRepository;
import com.woobeee.auth.repository.UserAuthRepository;
import com.woobeee.auth.repository.UserCredentialRepository;
import com.woobeee.auth.store.RefreshTokenStore;
import com.woobeee.auth.token.AccessTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserCredentialRepository userCredentialRepository;
    private final UserAuthRepository userAuthRepository;
    private final AuthRepository authRepository;

    @Override
    public IssuedAuthTokens issueTokens(String loginId, List<AuthType> authTypes) {
        String accessToken = accessTokenProvider.generateAccessToken(authTypes, loginId);

        UUID refreshTokenId = UUID.randomUUID();
        String refreshToken = accessTokenProvider.generateRefreshToken(loginId, refreshTokenId);

        refreshTokenStore.save(
                refreshTokenId,
                loginId,
                hashToken(refreshToken),
                Duration.ofMillis(accessTokenProvider.getRefreshTokenExpirationMillis())
        );

        return new IssuedAuthTokens(
                new AuthTokenResponse(
                        accessToken,
                        "Bearer",
                        accessTokenProvider.getAccessTokenExpirationMillis()
                ),
                refreshToken
        );
    }

    @Override
    public IssuedAuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JwtNotValidException(ErrorCode.login_refreshTokenMissing);
        }

        AccessTokenProvider.RefreshTokenPayload payload = accessTokenProvider.parseRefreshToken(refreshToken);
        RefreshTokenStore.StoredRefreshToken savedToken = refreshTokenStore.find(payload.tokenId())
                .orElseThrow(() -> new JwtNotValidException(ErrorCode.login_refreshTokenNotFound));

        if (!savedToken.loginId().equals(payload.loginId())
                || !savedToken.tokenHash().equals(hashToken(refreshToken))) {
            refreshTokenStore.delete(payload.tokenId());
            throw new JwtNotValidException(ErrorCode.login_jwtInvalid);
        }

        refreshTokenStore.delete(payload.tokenId());
        return issueTokens(payload.loginId(), resolveAuthTypes(payload.loginId()));
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            AccessTokenProvider.RefreshTokenPayload payload = accessTokenProvider.parseRefreshToken(refreshToken);
            refreshTokenStore.delete(payload.tokenId());
        } catch (JwtExpiredException | JwtNotValidException ex) {
            log.debug("Skipping refresh token revoke for invalid token: {}", ex.getMessage());
        }
    }

    private List<AuthType> resolveAuthTypes(String loginId) {
        UserCredential userCredential = userCredentialRepository.findUserCredentialByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.login_userNotFound));

        List<Long> userAuthIds = userAuthRepository.findAllById_UserId(userCredential.getId())
                .stream()
                .map(UserAuth::getAuthId)
                .toList();

        return authRepository.findAllById(userAuthIds)
                .stream()
                .map(Auth::getAuthType)
                .toList();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash refresh token", ex);
        }
    }
}
