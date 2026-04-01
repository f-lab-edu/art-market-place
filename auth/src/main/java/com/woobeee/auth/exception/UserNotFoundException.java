package com.woobeee.auth.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(ErrorCode message) {
        super(message.name());
    }
}
