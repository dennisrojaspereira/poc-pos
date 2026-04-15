package com.poc.pos.interfaces.api;

import com.poc.pos.application.service.AuthorizeTransactionUseCase;
import com.poc.pos.application.service.ConfirmTransactionUseCase;
import com.poc.pos.application.service.VoidTransactionUseCase;
import com.poc.pos.security.HmacAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@Import({RestExceptionHandler.class, HmacAuthenticationFilter.class})
@TestPropertySource(properties = "app.security.hmac.secret=test-secret")
class HmacAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizeTransactionUseCase authorizeTransactionUseCase;

    @MockBean
    private ConfirmTransactionUseCase confirmTransactionUseCase;

    @MockBean
    private VoidTransactionUseCase voidTransactionUseCase;

    @Test
    void shouldRejectRequestWithInvalidSignature() throws Exception {
        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"tx-1","terminalId":"term-1","nsu":"nsu-1","amount":10.00}
                                """.trim())
                        .header("X-Timestamp", Instant.now().toString())
                        .header("X-Correlation-Id", "corr-1")
                        .header("X-Signature", "invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptRequestWithValidSignature() throws Exception {
        when(authorizeTransactionUseCase.execute(any())).thenReturn(
                com.poc.pos.domain.model.Transaction.authorize("tx-1", "term-1", "nsu-1", new java.math.BigDecimal("10.00"))
        );

        String body = """
                {"transactionId":"tx-1","terminalId":"term-1","nsu":"nsu-1","amount":10.00}
                """.trim();
        String timestamp = Instant.now().toString();

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", "corr-1")
                        .header("X-Signature", sign("POST", "/authorize", timestamp, "corr-1", body)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTamperedBodyInMitmScenario() throws Exception {
        String originalBody = """
                {"transactionId":"tx-1","terminalId":"term-1","nsu":"nsu-1","amount":10.00}
                """.trim();
        String tamperedBody = """
                {"transactionId":"tx-1","terminalId":"term-1","nsu":"nsu-1","amount":999.99}
                """.trim();
        String timestamp = Instant.now().toString();

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedBody)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", "corr-mitm")
                        .header("X-Signature", sign("POST", "/authorize", timestamp, "corr-mitm", originalBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptReplayOfSignedRequestWithinAllowedWindow() throws Exception {
        when(authorizeTransactionUseCase.execute(any())).thenReturn(
                com.poc.pos.domain.model.Transaction.authorize("tx-1", "term-1", "nsu-1", new java.math.BigDecimal("10.00"))
        );

        String body = """
                {"transactionId":"tx-1","terminalId":"term-1","nsu":"nsu-1","amount":10.00}
                """.trim();
        String timestamp = Instant.now().toString();
        String signature = sign("POST", "/authorize", timestamp, "corr-replay", body);

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", "corr-replay")
                        .header("X-Signature", signature))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-replay"));

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", "corr-replay")
                        .header("X-Signature", signature))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-replay"));
    }

    private String sign(String method, String path, String timestamp, String correlationId, String body) throws Exception {
        String canonicalPayload = method + "\n" + path + "\n" + timestamp + "\n" + correlationId + "\n" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
    }
}
