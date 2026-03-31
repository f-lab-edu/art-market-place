package com.woobeee.auth.exception;

public class JwtNotValidException extends RuntimeException {
    public JwtNotValidException(ErrorCode message) {
        super(message.name());

    }
}
