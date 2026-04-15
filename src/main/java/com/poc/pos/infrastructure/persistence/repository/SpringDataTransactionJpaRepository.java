package com.poc.pos.infrastructure.persistence.repository;

import com.poc.pos.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataTransactionJpaRepository extends JpaRepository<TransactionJpaEntity, String> {

    Optional<TransactionJpaEntity> findByTerminalIdAndNsu(String terminalId, String nsu);
}
