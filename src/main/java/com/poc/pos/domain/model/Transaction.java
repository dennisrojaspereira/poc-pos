package com.poc.pos.domain.model;

import com.poc.pos.domain.exception.InvalidTransactionStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Transaction {

    private final String transactionId;
    private final String terminalId;
    private final String nsu;
    private final BigDecimal amount;
    private TransactionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Transaction(
            String transactionId,
            String terminalId,
            String nsu,
            BigDecimal amount,
            TransactionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.transactionId = requireText(transactionId, "transactionId");
        this.terminalId = requireText(terminalId, "terminalId");
        this.nsu = requireText(nsu, "nsu");
        this.amount = requirePositive(amount);
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Transaction authorize(String transactionId, String terminalId, String nsu, BigDecimal amount) {
        Instant now = Instant.now();
        return new Transaction(
                transactionId,
                terminalId,
                nsu,
                amount,
                TransactionStatus.AUTHORIZED,
                now,
                now
        );
    }

    public void confirm() {
        if (status != TransactionStatus.AUTHORIZED) {
            throw new InvalidTransactionStateException(
                    "Only AUTHORIZED transactions can be confirmed. Current status: " + status
            );
        }
        status = TransactionStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    public void voidTransaction() {
        if (status == TransactionStatus.VOIDED) {
            throw new InvalidTransactionStateException("VOIDED transactions cannot be voided again");
        }
        status = TransactionStatus.VOIDED;
        updatedAt = Instant.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getNsu() {
        return nsu;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        Objects.requireNonNull(value, "amount is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return value;
    }
}
