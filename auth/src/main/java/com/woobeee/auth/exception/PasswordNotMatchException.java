package com.woobeee.auth.exception;

public class PasswordNotMatchException extends RuntimeException {
    public PasswordNotMatchException(ErrorCode message) {
        super(message.name());
    }
}
