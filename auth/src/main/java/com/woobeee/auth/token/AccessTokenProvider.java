package com.woobeee.auth.token;

import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.ErrorCode;
import com.woobeee.auth.exception.JwtExpiredException;
import com.woobeee.auth.exception.JwtNotValidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class AccessTokenProvider {
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Getter
    private final long accessTokenExpirationMillis;
    @Getter
    private final long refreshTokenExpirationMillis;
    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;

    public AccessTokenProvider(
            @Value("${jwt.access-token.secret}") String accessSecret,
            @Value("${jwt.refresh-token.secret}") String refreshSecret,
            @Value("${jwt.access-token.expiration-millis:3600000}") long accessTokenExpirationMillis,
            @Value("${jwt.refresh-token.expiration-millis:86400000}") long refreshTokenExpirationMillis
    ) {
        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String generateAccessToken(List<AuthType> authTypes, String loginId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMillis);

        return Jwts.builder()
                .subject(loginId)
                .claim("roles", authTypes.stream().map(AuthType::name).toList())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessTokenKey)
                .compact();
    }

    public String generateRefreshToken(String loginId, UUID tokenId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMillis);

        return Jwts.builder()
                .id(tokenId.toString())
                .subject(loginId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(refreshTokenKey)
                .compact();
    }

    public RefreshTokenPayload parseRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken, refreshTokenKey);
        validateTokenType(claims);

        try {
            return new RefreshTokenPayload(
                    claims.getSubject(),
                    UUID.fromString(claims.getId())
            );
        } catch (Exception ex) {
            throw new JwtNotValidException(ErrorCode.login_jwtInvalid);
        }
    }

    private Claims parseClaims(String token, SecretKey key) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtExpiredException(ErrorCode.login_jwtExpired);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtNotValidException(ErrorCode.login_jwtInvalid);
        }
    }

    private void validateTokenType(Claims claims) {
        String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!AccessTokenProvider.REFRESH_TOKEN_TYPE.equals(actualType)) {
            throw new JwtNotValidException(ErrorCode.login_jwtInvalid);
        }
    }

    public record RefreshTokenPayload(
            String loginId,
            UUID tokenId
    ) {
    }
}