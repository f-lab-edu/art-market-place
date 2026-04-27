package com.woobeee.artmarketplace.blog.exception;

public class CustomInternalServerException extends RuntimeException {
    public CustomInternalServerException(ErrorCode message) {
        super(message.name());
    }
}
