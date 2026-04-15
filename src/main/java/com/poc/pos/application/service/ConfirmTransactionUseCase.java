package com.poc.pos.application.service;

import com.poc.pos.application.command.ConfirmTransactionCommand;
import com.poc.pos.domain.exception.InvalidTransactionStateException;
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
public class ConfirmTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final PaymentProcessorPort paymentProcessorPort;
    private final TransactionMetricsRecorder metricsRecorder;
    private final ProcessTracing processTracing;

    public ConfirmTransactionUseCase(
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
    public void execute(ConfirmTransactionCommand command) {
        processTracing.traceVoid(
                "pos.confirm",
                "confirm transaction",
                "confirm",
                "usecase",
                () -> executeInternal(command)
        );
    }

    private void executeInternal(ConfirmTransactionCommand command) {
        Transaction transaction = processTracing.trace(
                "pos.confirm.lookup-transaction-id",
                "confirm lookup by transaction id",
                "confirm",
                "lookup_transaction_id",
                () -> transactionRepository.findByTransactionId(command.transactionId())
                        .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"))
        );

        processTracing.traceVoid(
                "pos.confirm.validate-state",
                "confirm validate state",
                "confirm",
                "validate_state",
                () -> validateState(transaction)
        );

        processTracing.traceVoid(
                "pos.confirm.payment-processor",
                "confirm payment processor",
                "confirm",
                "payment_processor",
                () -> paymentProcessorPort.confirm(command)
        );
        processTracing.traceVoid(
                "pos.confirm.domain-transition",
                "confirm domain transition",
                "confirm",
                "domain_transition",
                transaction::confirm
        );
        processTracing.trace(
                "pos.confirm.persist",
                "confirm persist transaction",
                "confirm",
                "persist",
                () -> transactionRepository.save(transaction)
        );
        metricsRecorder.record("confirm", "success");
    }

    private void validateState(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.CONFIRMED) {
            metricsRecorder.record("confirm", "idempotent_hit");
            return;
        }
        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            metricsRecorder.record("confirm", "invalid_state");
            throw new InvalidTransactionStateException("VOIDED transactions cannot be confirmed");
        }
    }
}
