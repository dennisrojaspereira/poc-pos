package com.poc.pos.interfaces.api.dto;

import com.poc.pos.domain.model.TransactionStatus;

import java.math.BigDecimal;

public record AuthorizeResponse(
        String transactionId,
        String terminalId,
        String nsu,
        BigDecimal amount,
        TransactionStatus status
) {
}
