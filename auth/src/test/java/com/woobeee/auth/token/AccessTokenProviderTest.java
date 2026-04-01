package com.woobeee.auth.token;

import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.JwtExpiredException;
import com.woobeee.auth.exception.JwtNotValidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenProviderTest {
    private static final String ACCESS_SECRET = "01234567890123456789012345678901";
    private static final String REFRESH_SECRET = "abcdefghijklmnopqrstuvwxyz123456";

    @Test
    void generateAccessToken_containsExpectedClaims() {
        AccessTokenProvider provider = new AccessTokenProvider(
                ACCESS_SECRET,
                REFRESH_SECRET,
                3_600_000L,
                86_400_000L
        );

        String token = provider.generateAccessToken(
                List.of(AuthType.ROLE_MEMBER, AuthType.ROLE_ADMIN),
                "helloJosh"
        );

        Claims claims = parseClaims(token, ACCESS_SECRET);

        assertThat(claims.getSubject()).isEqualTo("helloJosh");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("roles", List.class))
                .containsExactly("ROLE_MEMBER", "ROLE_ADMIN");
        assertThat(claims.getIssuedAt()).isBefore(claims.getExpiration());
    }

    @Test
    void generateRefreshToken_andParseRefreshToken_returnsPayload() {
        AccessTokenProvider provider = new AccessTokenProvider(
                ACCESS_SECRET,
                REFRESH_SECRET,
                3_600_000L,
                86_400_000L
        );
        UUID tokenId = UUID.randomUUID();

        String refreshToken = provider.generateRefreshToken("helloJosh", tokenId);

        AccessTokenProvider.RefreshTokenPayload payload = provider.parseRefreshToken(refreshToken);

        assertThat(payload.loginId()).isEqualTo("helloJosh");
        assertThat(payload.tokenId()).isEqualTo(tokenId);
    }

    @Test
    void parseRefreshToken_withAccessToken_throwsJwtNotValidException() {
        AccessTokenProvider provider = new AccessTokenProvider(
                ACCESS_SECRET,
                REFRESH_SECRET,
                3_600_000L,
                86_400_000L
        );
        String accessToken = provider.generateAccessToken(List.of(AuthType.ROLE_MEMBER), "helloJosh");

        assertThatThrownBy(() -> provider.parseRefreshToken(accessToken))
                .isInstanceOf(JwtNotValidException.class)
                .hasMessage("login_jwtInvalid");
    }

    @Test
    void parseRefreshToken_withExpiredToken_throwsJwtExpiredException() {
        AccessTokenProvider provider = new AccessTokenProvider(
                ACCESS_SECRET,
                REFRESH_SECRET,
                3_600_000L,
                -1_000L
        );
        String expiredToken = provider.generateRefreshToken("helloJosh", UUID.randomUUID());

        assertThatThrownBy(() -> provider.parseRefreshToken(expiredToken))
                .isInstanceOf(JwtExpiredException.class)
                .hasMessage("login_jwtExpired");
    }

    @Test
    void parseRefreshToken_withInvalidUuid_throwsJwtNotValidException() {
        AccessTokenProvider provider = new AccessTokenProvider(
                ACCESS_SECRET,
                REFRESH_SECRET,
                3_600_000L,
                86_400_000L
        );
        String invalidRefreshToken = Jwts.builder()
                .id("not-a-uuid")
                .subject("helloJosh")
                .claim("type", "refresh")
                .signWith(secretKey(REFRESH_SECRET))
                .compact();

        assertThatThrownBy(() -> provider.parseRefreshToken(invalidRefreshToken))
                .isInstanceOf(JwtNotValidException.class)
                .hasMessage("login_jwtInvalid");
    }

    private static Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
