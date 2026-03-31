package com.woobeee.auth.exception;

public class JwtExpiredException extends RuntimeException {
    public JwtExpiredException(ErrorCode message) {
        super(message.name());
    }
}
