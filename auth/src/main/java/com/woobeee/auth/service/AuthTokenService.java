package com.woobeee.auth.service;

import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.entity.enums.AuthType;

import java.util.List;

public interface AuthTokenService {
    IssuedAuthTokens issueTokens(String loginId, List<AuthType> authTypes);
    IssuedAuthTokens refresh(String refreshToken);
    void revoke(String refreshToken);
}
