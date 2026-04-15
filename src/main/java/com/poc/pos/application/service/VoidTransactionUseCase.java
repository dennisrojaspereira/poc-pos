package com.poc.pos.application.service;

import com.poc.pos.application.command.VoidTransactionCommand;
import com.poc.pos.domain.exception.TransactionNotFoundException;
import com.poc.pos.domain.model.Transaction;
import com.poc.pos.domain.model.TransactionStatus;
import com.poc.pos.domain.port.PaymentProcessorPort;
import com.poc.pos.domain.port.TransactionRepository;
import com.poc.pos.monitoring.ProcessTracing;
import com.poc.pos.monitoring.TransactionMetricsRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoidTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final PaymentProcessorPort paymentProcessorPort;
    private final TransactionMetricsRecorder metricsRecorder;
    private final ProcessTracing processTracing;

    public VoidTransactionUseCase(
            TransactionRepository transactionRepository,
            PaymentProcessorPort paymentProcessorPort,
            TransactionMetricsRecorder metricsRecorder,
            ProcessTracing processTracing
    ) {
        this.transactionRepository = transactionRepository;
        this.paymentProcessorPort = paymentProcessorPort;
        this.metricsRecorder = metricsRecorder;
        this.processTracing = processTracing;
    }

    @Transactional
    public void execute(VoidTransactionCommand command) {
        processTracing.traceVoid(
                "pos.void",
                "void transaction",
                "void",
                "usecase",
                () -> executeInternal(command)
        );
    }

    private void executeInternal(VoidTransactionCommand command) {
        Transaction transaction = processTracing.trace(
                "pos.void.resolve-transaction",
                "void resolve transaction",
                "void",
                "resolve_transaction",
                () -> resolve(command)
        );

        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            metricsRecorder.record("void", "idempotent_hit");
            return;
        }

        processTracing.traceVoid(
                "pos.void.payment-processor",
                "void payment processor",
                "void",
                "payment_processor",
                () -> paymentProcessorPort.voidTransaction(command)
        );
        processTracing.traceVoid(
                "pos.void.domain-transition",
                "void domain transition",
                "void",
                "domain_transition",
                transaction::voidTransaction
        );
        processTracing.trace(
                "pos.void.persist",
                "void persist transaction",
                "void",
                "persist",
                () -> transactionRepository.save(transaction)
        );
        metricsRecorder.record("void", "success");
    }

    private Transaction resolve(VoidTransactionCommand command) {
        if (command.transactionId() != null && !command.transactionId().isBlank()) {
            return transactionRepository.findByTransactionId(command.transactionId())
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        }
        if (command.terminalId() != null && !command.terminalId().isBlank()
                && command.nsu() != null && !command.nsu().isBlank()) {
            return transactionRepository.findByTerminalIdAndNsu(command.terminalId(), command.nsu())
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        }
        throw new IllegalArgumentException("transactionId or terminalId + nsu is required");
    }
}
