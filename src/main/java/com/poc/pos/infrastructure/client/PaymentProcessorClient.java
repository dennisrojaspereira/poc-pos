package com.poc.pos.infrastructure.client;

import com.poc.pos.application.command.AuthorizeTransactionCommand;
import com.poc.pos.application.command.ConfirmTransactionCommand;
import com.poc.pos.application.command.VoidTransactionCommand;
import com.poc.pos.domain.port.PaymentProcessorPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentProcessorClient implements PaymentProcessorPort {

    private final RestClient restClient;
    private final String authorizePath;
    private final String confirmPath;
    private final String voidPath;

    public PaymentProcessorClient(
            RestClient paymentProcessorRestClient,
            @Value("${app.payment-processor.paths.authorize:/authorize}") String authorizePath,
            @Value("${app.payment-processor.paths.confirm:/confirm}") String confirmPath,
            @Value("${app.payment-processor.paths.void:/void}") String voidPath
    ) {
        this.restClient = paymentProcessorRestClient;
        this.authorizePath = authorizePath;
        this.confirmPath = confirmPath;
        this.voidPath = voidPath;
    }

    @Override
    @Retry(name = "paymentProcessor")
    @CircuitBreaker(name = "paymentProcessor")
    @Bulkhead(name = "paymentProcessor", type = Bulkhead.Type.SEMAPHORE)
    public void authorize(AuthorizeTransactionCommand command) {
        post(authorizePath, command);
    }

    @Override
    @Retry(name = "paymentProcessor")
    @CircuitBreaker(name = "paymentProcessor")
    @Bulkhead(name = "paymentProcessor", type = Bulkhead.Type.SEMAPHORE)
    public void confirm(ConfirmTransactionCommand command) {
        post(confirmPath, command);
    }

    @Override
    @Retry(name = "paymentProcessor")
    @CircuitBreaker(name = "paymentProcessor")
    @Bulkhead(name = "paymentProcessor", type = Bulkhead.Type.SEMAPHORE)
    public void voidTransaction(VoidTransactionCommand command) {
        post(voidPath, command);
    }

    private void post(String path, Object body) {
        restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
