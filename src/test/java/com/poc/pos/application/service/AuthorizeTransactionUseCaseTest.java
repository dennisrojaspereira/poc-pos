package com.poc.pos.application.service;

import com.poc.pos.application.command.AuthorizeTransactionCommand;
import com.poc.pos.application.command.ConfirmTransactionCommand;
import com.poc.pos.application.command.VoidTransactionCommand;
import com.poc.pos.domain.exception.UniqueConstraintViolationException;
import com.poc.pos.domain.model.Transaction;
import com.poc.pos.domain.port.PaymentProcessorPort;
import com.poc.pos.domain.port.TransactionRepository;
import com.poc.pos.monitoring.ProcessTracing;
import com.poc.pos.monitoring.TransactionMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizeTransactionUseCaseTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ProcessTracing processTracing = new ProcessTracing(ObservationRegistry.create(), meterRegistry);

    @Test
    void shouldReturnExistingTransactionOnRepeatedAuthorizeWithoutCallingProcessorAgain() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        CountingPaymentProcessor paymentProcessor = new CountingPaymentProcessor();
        AuthorizeTransactionUseCase useCase = new AuthorizeTransactionUseCase(
                repository,
                paymentProcessor,
                new TransactionMetricsRecorder(meterRegistry),
                processTracing
        );
        AuthorizeTransactionCommand command = new AuthorizeTransactionCommand(
                "tx-1",
                "term-1",
                "nsu-1",
                new BigDecimal("10.00")
        );

        Transaction first = useCase.execute(command);
        Transaction second = useCase.execute(command);

        assertThat(first.getTransactionId()).isEqualTo(second.getTransactionId());
        assertThat(paymentProcessor.authorizeCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldRereadOnUniqueConflictDuringConcurrentAuthorize() throws Exception {
        ConcurrentConflictRepository repository = new ConcurrentConflictRepository();
        CountingPaymentProcessor paymentProcessor = new CountingPaymentProcessor();
        AuthorizeTransactionUseCase useCase = new AuthorizeTransactionUseCase(
                repository,
                paymentProcessor,
                new TransactionMetricsRecorder(meterRegistry),
                processTracing
        );
        AuthorizeTransactionCommand command = new AuthorizeTransactionCommand(
                "tx-2",
                "term-2",
                "nsu-2",
                new BigDecimal("15.00")
        );

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Transaction> first = executor.submit(() -> useCase.execute(command));
            Future<Transaction> second = executor.submit(() -> useCase.execute(command));

            Transaction firstResult = first.get(5, TimeUnit.SECONDS);
            Transaction secondResult = second.get(5, TimeUnit.SECONDS);

            assertThat(firstResult.getTransactionId()).isEqualTo("tx-2");
            assertThat(secondResult.getTransactionId()).isEqualTo("tx-2");
            assertThat(repository.findByTerminalIdAndNsu("term-2", "nsu-2")).isPresent();
        }
    }

    private static class CountingPaymentProcessor implements PaymentProcessorPort {

        private final AtomicInteger authorizeCalls = new AtomicInteger();

        @Override
        public void authorize(AuthorizeTransactionCommand command) {
            authorizeCalls.incrementAndGet();
        }

        @Override
        public void confirm(ConfirmTransactionCommand command) {
        }

        @Override
        public void voidTransaction(VoidTransactionCommand command) {
        }
    }

    private static class InMemoryTransactionRepository implements TransactionRepository {

        protected final Map<String, Transaction> byTransactionId = new ConcurrentHashMap<>();
        protected final Map<String, Transaction> byTerminalAndNsu = new ConcurrentHashMap<>();

        @Override
        public Transaction save(Transaction transaction) {
            byTransactionId.put(transaction.getTransactionId(), transaction);
            byTerminalAndNsu.put(key(transaction.getTerminalId(), transaction.getNsu()), transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findByTransactionId(String transactionId) {
            return Optional.ofNullable(byTransactionId.get(transactionId));
        }

        @Override
        public Optional<Transaction> findByTerminalIdAndNsu(String terminalId, String nsu) {
            return Optional.ofNullable(byTerminalAndNsu.get(key(terminalId, nsu)));
        }

        protected String key(String terminalId, String nsu) {
            return terminalId + "::" + nsu;
        }
    }

    private static final class ConcurrentConflictRepository extends InMemoryTransactionRepository {

        private final CountDownLatch concurrentReadBarrier = new CountDownLatch(2);
        private final AtomicInteger saveAttempts = new AtomicInteger();

        @Override
        public Optional<Transaction> findByTerminalIdAndNsu(String terminalId, String nsu) {
            if (super.findByTerminalIdAndNsu(terminalId, nsu).isEmpty()) {
                concurrentReadBarrier.countDown();
                awaitBarrier();
                return Optional.empty();
            }
            return super.findByTerminalIdAndNsu(terminalId, nsu);
        }

        @Override
        public Transaction save(Transaction transaction) {
            if (saveAttempts.incrementAndGet() == 1) {
                return super.save(transaction);
            }
            throw new UniqueConstraintViolationException("duplicate", new RuntimeException("duplicate"));
        }

        private void awaitBarrier() {
            try {
                concurrentReadBarrier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
        }
    }
}
