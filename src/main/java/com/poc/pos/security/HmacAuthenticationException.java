package com.poc.pos.security;

public class HmacAuthenticationException extends RuntimeException {

    public HmacAuthenticationException(String message) {
        super(message);
    }
}
