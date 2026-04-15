package com.poc.pos.domain.port;

import com.poc.pos.domain.model.Transaction;

import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByTerminalIdAndNsu(String terminalId, String nsu);
}
