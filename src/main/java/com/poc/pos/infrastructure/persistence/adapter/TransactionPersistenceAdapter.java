package com.poc.pos.infrastructure.persistence.adapter;

import com.poc.pos.domain.exception.UniqueConstraintViolationException;
import com.poc.pos.domain.model.Transaction;
import com.poc.pos.domain.port.TransactionRepository;
import com.poc.pos.infrastructure.persistence.entity.TransactionJpaEntity;
import com.poc.pos.infrastructure.persistence.repository.SpringDataTransactionJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final SpringDataTransactionJpaRepository repository;

    public TransactionPersistenceAdapter(SpringDataTransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        try {
            TransactionJpaEntity persisted = repository.saveAndFlush(toEntity(transaction));
            return toDomain(persisted);
        } catch (DataIntegrityViolationException exception) {
            throw new UniqueConstraintViolationException("Transaction unique constraint violation", exception);
        }
    }

    @Override
    public Optional<Transaction> findByTransactionId(String transactionId) {
        return repository.findById(transactionId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByTerminalIdAndNsu(String terminalId, String nsu) {
        return repository.findByTerminalIdAndNsu(terminalId, nsu)
                .map(this::toDomain);
    }

    private TransactionJpaEntity toEntity(Transaction transaction) {
        return new TransactionJpaEntity(
                transaction.getTransactionId(),
                transaction.getTerminalId(),
                transaction.getNsu(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getTransactionId(),
                entity.getTerminalId(),
                entity.getNsu(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
