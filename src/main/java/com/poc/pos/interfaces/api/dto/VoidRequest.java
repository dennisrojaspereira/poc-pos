package com.poc.pos.interfaces.api.dto;

public record VoidRequest(
        String transactionId,
        String terminalId,
        String nsu
) {
}
