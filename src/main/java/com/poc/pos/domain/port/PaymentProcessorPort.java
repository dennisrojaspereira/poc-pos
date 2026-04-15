package com.poc.pos.domain.port;

import com.poc.pos.application.command.AuthorizeTransactionCommand;
import com.poc.pos.application.command.ConfirmTransactionCommand;
import com.poc.pos.application.command.VoidTransactionCommand;

public interface PaymentProcessorPort {

    void authorize(AuthorizeTransactionCommand command);

    void confirm(ConfirmTransactionCommand command);

    void voidTransaction(VoidTransactionCommand command);
}
