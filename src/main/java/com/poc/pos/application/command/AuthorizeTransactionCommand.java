package com.poc.pos.application.command;

import java.math.BigDecimal;

public record AuthorizeTransactionCommand(
        String transactionId,
        String terminalId,
        String nsu,
        BigDecimal amount
) {
}
