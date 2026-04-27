package com.woobeee.artmarketplace.blog.exception;

public class CustomConflictException extends RuntimeException{
    public CustomConflictException(ErrorCode message) {
        super(message.name());
    }
}
