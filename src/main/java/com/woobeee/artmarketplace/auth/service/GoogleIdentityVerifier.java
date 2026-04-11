package com.woobeee.artmarketplace.auth.service;

import com.woobeee.artmarketplace.auth.service.dto.GoogleIdentity;

public interface GoogleIdentityVerifier {
    GoogleIdentity verify(String idToken);
}
