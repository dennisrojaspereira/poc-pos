package com.poc.pos.interfaces.api;

import com.poc.pos.application.command.AuthorizeTransactionCommand;
import com.poc.pos.application.command.ConfirmTransactionCommand;
import com.poc.pos.application.command.VoidTransactionCommand;
import com.poc.pos.application.service.AuthorizeTransactionUseCase;
import com.poc.pos.application.service.ConfirmTransactionUseCase;
import com.poc.pos.application.service.VoidTransactionUseCase;
import com.poc.pos.domain.model.Transaction;
import com.poc.pos.interfaces.api.dto.AuthorizeRequest;
import com.poc.pos.interfaces.api.dto.AuthorizeResponse;
import com.poc.pos.interfaces.api.dto.ConfirmRequest;
import com.poc.pos.interfaces.api.dto.VoidRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final AuthorizeTransactionUseCase authorizeTransactionUseCase;
    private final ConfirmTransactionUseCase confirmTransactionUseCase;
    private final VoidTransactionUseCase voidTransactionUseCase;

    public TransactionController(
            AuthorizeTransactionUseCase authorizeTransactionUseCase,
            ConfirmTransactionUseCase confirmTransactionUseCase,
            VoidTransactionUseCase voidTransactionUseCase
    ) {
        this.authorizeTransactionUseCase = authorizeTransactionUseCase;
        this.confirmTransactionUseCase = confirmTransactionUseCase;
        this.voidTransactionUseCase = voidTransactionUseCase;
    }

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(@Valid @RequestBody AuthorizeRequest request) {
        Transaction transaction = authorizeTransactionUseCase.execute(new AuthorizeTransactionCommand(
                request.transactionId(),
                request.terminalId(),
                request.nsu(),
                request.amount()
        ));
        return ResponseEntity.ok(new AuthorizeResponse(
                transaction.getTransactionId(),
                transaction.getTerminalId(),
                transaction.getNsu(),
                transaction.getAmount(),
                transaction.getStatus()
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmRequest request) {
        confirmTransactionUseCase.execute(new ConfirmTransactionCommand(request.transactionId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/void")
    public ResponseEntity<Void> voidTransaction(@Valid @RequestBody VoidRequest request) {
        voidTransactionUseCase.execute(new VoidTransactionCommand(
                request.transactionId(),
                request.terminalId(),
                request.nsu()
        ));
        return ResponseEntity.noContent().build();
    }
}
