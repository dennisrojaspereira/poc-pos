package com.poc.merchant.interfaces.api.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
