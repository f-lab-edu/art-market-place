package com.woobeee.auth.service;

import com.woobeee.auth.dto.response.IssuedAuthTokens;

public interface OauthUserCredentialService {
    IssuedAuthTokens signIn(String idTokenString);
    IssuedAuthTokens logIn(String idTokenString);
    IssuedAuthTokens refresh(String refreshToken);
    void logout(String refreshToken);
}
