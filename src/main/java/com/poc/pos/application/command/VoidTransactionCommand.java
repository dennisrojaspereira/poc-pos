package com.poc.pos.application.command;

public record VoidTransactionCommand(
        String transactionId,
        String terminalId,
        String nsu
) {
}
