package com.poc.merchant.domain.exception;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String message) {
        super(message);
    }
}
