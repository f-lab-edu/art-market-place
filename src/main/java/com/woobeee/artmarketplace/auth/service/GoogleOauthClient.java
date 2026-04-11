package com.woobeee.artmarketplace.auth.service;

import com.woobeee.artmarketplace.auth.service.dto.GoogleTokenExchangeResponse;

public interface GoogleOauthClient {
    GoogleTokenExchangeResponse exchangeAuthorizationCode(String code, String codeVerifier);
}
