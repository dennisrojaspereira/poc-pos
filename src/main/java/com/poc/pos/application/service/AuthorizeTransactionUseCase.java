package com.poc.pos.application.service;

import com.poc.pos.application.command.AuthorizeTransactionCommand;
import com.poc.pos.domain.exception.IdempotencyConflictException;
import com.poc.pos.domain.exception.UniqueConstraintViolationException;
import com.poc.pos.domain.model.Transaction;
import com.poc.pos.domain.port.PaymentProcessorPort;
import com.poc.pos.domain.port.TransactionRepository;
import com.poc.pos.monitoring.ProcessTracing;
import com.poc.pos.monitoring.TransactionMetricsRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizeTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final PaymentProcessorPort paymentProcessorPort;
    private final TransactionMetricsRecorder metricsRecorder;
    private final ProcessTracing processTracing;

    public AuthorizeTransactionUseCase(
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
    public Transaction execute(AuthorizeTransactionCommand command) {
        return processTracing.trace(
                "pos.authorize",
                "authorize transaction",
                "authorize",
                "usecase",
                () -> executeInternal(command)
        );
    }

    private Transaction executeInternal(AuthorizeTransactionCommand command) {
        Transaction existingByTerminalAndNsu = processTracing.trace(
                "pos.authorize.idempotency.lookup-terminal-nsu",
                "authorize lookup by terminal and nsu",
                "authorize",
                "lookup_terminal_nsu",
                () -> transactionRepository.findByTerminalIdAndNsu(command.terminalId(), command.nsu()).orElse(null)
        );
        if (existingByTerminalAndNsu != null) {
            metricsRecorder.record("authorize", "idempotent_hit");
            return ensureIdempotent(existingByTerminalAndNsu, command);
        }

        Transaction existingByTransactionId = processTracing.trace(
                "pos.authorize.idempotency.lookup-transaction-id",
                "authorize lookup by transaction id",
                "authorize",
                "lookup_transaction_id",
                () -> transactionRepository.findByTransactionId(command.transactionId()).orElse(null)
        );
        if (existingByTransactionId != null) {
            metricsRecorder.record("authorize", "idempotent_hit");
            return ensureIdempotent(existingByTransactionId, command);
        }

        processTracing.traceVoid(
                "pos.authorize.payment-processor",
                "authorize payment processor",
                "authorize",
                "payment_processor",
                () -> paymentProcessorPort.authorize(command)
        );

        Transaction transaction = processTracing.trace(
                "pos.authorize.domain-create",
                "authorize domain create",
                "authorize",
                "domain_create",
                () -> Transaction.authorize(
                        command.transactionId(),
                        command.terminalId(),
                        command.nsu(),
                        command.amount()
                )
        );

        try {
            Transaction saved = processTracing.trace(
                    "pos.authorize.persist",
                    "authorize persist transaction",
                    "authorize",
                    "persist",
                    () -> transactionRepository.save(transaction)
            );
            metricsRecorder.record("authorize", "success");
            return saved;
        } catch (UniqueConstraintViolationException exception) {
            metricsRecorder.record("authorize", "reread_on_conflict");
            return processTracing.trace(
                    "pos.authorize.reread-on-conflict",
                    "authorize reread on conflict",
                    "authorize",
                    "reread_on_conflict",
                    () -> transactionRepository.findByTerminalIdAndNsu(command.terminalId(), command.nsu())
                            .map(existing -> ensureIdempotent(existing, command))
                            .or(() -> transactionRepository.findByTransactionId(command.transactionId())
                                    .map(existing -> ensureIdempotent(existing, command)))
                            .orElseThrow(() -> exception)
            );
        }
    }

    private Transaction ensureIdempotent(Transaction existing, AuthorizeTransactionCommand command) {
        return processTracing.trace(
                "pos.authorize.ensure-idempotent",
                "authorize ensure idempotent payload",
                "authorize",
                "ensure_idempotent",
                () -> {
                    boolean samePayload = existing.getTransactionId().equals(command.transactionId())
                            && existing.getTerminalId().equals(command.terminalId())
                            && existing.getNsu().equals(command.nsu())
                            && existing.getAmount().compareTo(command.amount()) == 0;

                    if (!samePayload) {
                        metricsRecorder.record("authorize", "idempotency_conflict");
                        throw new IdempotencyConflictException(
                                "Existing transaction for the idempotency key has a different payload"
                        );
                    }
                    return existing;
                }
        );
    }
}
