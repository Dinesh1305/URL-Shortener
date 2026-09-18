package com.demo.exception;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String message) {
        super(message);
    }
}
