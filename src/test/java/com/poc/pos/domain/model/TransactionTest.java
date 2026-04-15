package com.poc.pos.domain.model;

import com.poc.pos.domain.exception.InvalidTransactionStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void shouldCreateAuthorizedTransaction() {
        Transaction transaction = Transaction.authorize("tx-1", "term-1", "nsu-1", new BigDecimal("10.00"));

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        assertThat(transaction.getCreatedAt()).isNotNull();
        assertThat(transaction.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldConfirmAuthorizedTransaction() {
        Transaction transaction = Transaction.authorize("tx-1", "term-1", "nsu-1", new BigDecimal("10.00"));

        transaction.confirm();

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
    }

    @Test
    void shouldRejectConfirmWhenNotAuthorized() {
        Transaction transaction = Transaction.authorize("tx-1", "term-1", "nsu-1", new BigDecimal("10.00"));
        transaction.confirm();

        assertThatThrownBy(transaction::confirm)
                .isInstanceOf(InvalidTransactionStateException.class);
    }

    @Test
    void shouldVoidAuthorizedOrConfirmedTransaction() {
        Transaction authorized = Transaction.authorize("tx-1", "term-1", "nsu-1", new BigDecimal("10.00"));
        authorized.voidTransaction();

        Transaction confirmed = Transaction.authorize("tx-2", "term-2", "nsu-2", new BigDecimal("10.00"));
        confirmed.confirm();
        confirmed.voidTransaction();

        assertThat(authorized.getStatus()).isEqualTo(TransactionStatus.VOIDED);
        assertThat(confirmed.getStatus()).isEqualTo(TransactionStatus.VOIDED);
    }
}
