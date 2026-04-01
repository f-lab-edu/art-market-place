package com.woobeee.auth.exception;

public class CustomConflictException extends RuntimeException{
    public CustomConflictException(ErrorCode message) {
        super(message.name());
    }
}
