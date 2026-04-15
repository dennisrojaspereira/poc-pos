package com.poc.pos.interfaces.api.dto;

public record ErrorResponse(
        String code,
        String message,
        String correlationId
) {
}
