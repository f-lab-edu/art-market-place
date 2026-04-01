package com.woobeee.auth.exception;

public class UserConflictException extends RuntimeException {
    public UserConflictException(ErrorCode message) {
        super(message.name());
    }
}
