package com.poc.pos.infrastructure.persistence.entity;

import com.poc.pos.domain.model.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false, length = 64)
    private String transactionId;

    @Column(name = "terminal_id", nullable = false, updatable = false, length = 64)
    private String terminalId;

    @Column(name = "nsu", nullable = false, updatable = false, length = 64)
    private String nsu;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TransactionJpaEntity() {
    }

    public TransactionJpaEntity(
            String transactionId,
            String terminalId,
            String nsu,
            BigDecimal amount,
            TransactionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.transactionId = transactionId;
        this.terminalId = terminalId;
        this.nsu = nsu;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}
