package com.poc.pos.interfaces.api;

import com.poc.pos.domain.port.PaymentProcessorPort;
import com.poc.pos.infrastructure.persistence.repository.SpringDataTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.hmac.secret=test-secret",
        "spring.datasource.url=jdbc:h2:mem:hmac-mitm;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class HmacMitmE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataTransactionJpaRepository transactionRepository;

    @MockBean
    private PaymentProcessorPort paymentProcessorPort;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        doNothing().when(paymentProcessorPort).authorize(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectTamperedBodyBeforeReachingBusinessFlow() throws Exception {
        String originalBody = """
                {"transactionId":"tx-mitm-1","terminalId":"term-mitm","nsu":"nsu-mitm-1","amount":10.00}
                """.trim();
        String tamperedBody = """
                {"transactionId":"tx-mitm-1","terminalId":"term-mitm","nsu":"nsu-mitm-1","amount":999.99}
                """.trim();
        String timestamp = Instant.now().toString();
        String correlationId = "corr-mitm-e2e";

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedBody)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", correlationId)
                        .header("X-Feature-Variant", "control")
                        .header("X-Signature", sign("POST", "/authorize", timestamp, correlationId, originalBody)))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectReplayWithinAllowedWindow() throws Exception {
        String body = """
                {"transactionId":"tx-replay-1","terminalId":"term-replay","nsu":"nsu-replay-1","amount":10.00}
                """.trim();
        String timestamp = Instant.now().toString();
        String correlationId = "corr-replay-e2e";
        String signature = sign("POST", "/authorize", timestamp, correlationId, body);

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", correlationId)
                        .header("X-Feature-Variant", "control")
                        .header("X-Signature", signature))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", correlationId))
                .andExpect(jsonPath("$.transactionId").value("tx-replay-1"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        mockMvc.perform(post("/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-Timestamp", timestamp)
                        .header("X-Correlation-Id", correlationId)
                        .header("X-Feature-Variant", "control")
                        .header("X-Signature", signature))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(transactionRepository.count()).isEqualTo(1);
    }

    private String sign(String method, String path, String timestamp, String correlationId, String body) throws Exception {
        String canonicalPayload = method + "\n" + path + "\n" + timestamp + "\n" + correlationId + "\n" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
    }
}
