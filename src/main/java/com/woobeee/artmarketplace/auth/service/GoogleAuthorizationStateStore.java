package com.woobeee.artmarketplace.auth.service;

import java.util.Optional;

import com.woobeee.artmarketplace.auth.service.dto.GoogleAuthorizationContext;

public interface GoogleAuthorizationStateStore {
    void save(String state, GoogleAuthorizationContext context);

    Optional<GoogleAuthorizationContext> find(String state);

    void delete(String state);
}
