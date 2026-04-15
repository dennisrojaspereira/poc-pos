package com.poc.merchant.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpaAuthorizationClient {

    private final RestClient restClient;
    private final String policyUrl;

    public OpaAuthorizationClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.opa.url}") String policyUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.policyUrl = policyUrl;
    }

    public boolean isAllowed(String subject, List<String> roles, String method, String path) {
        Map<String, Object> body = Map.of(
                "input", Map.of(
                        "subject", subject,
                        "roles", roles,
                        "method", method,
                        "path", path
                )
        );

        OpaDecision decision = restClient.post()
                .uri(policyUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpaDecision.class);

        return decision != null && Boolean.TRUE.equals(decision.result());
    }

    public record OpaDecision(Boolean result) {
    }
}
